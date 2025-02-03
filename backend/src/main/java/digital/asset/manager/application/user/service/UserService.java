package digital.asset.manager.application.user.service;

import digital.asset.manager.application.common.exception.ApplicationException;
import digital.asset.manager.application.common.exception.ErrorCode;
import digital.asset.manager.application.global.oauth.domain.ProviderType;
import digital.asset.manager.application.user.domain.UserEntity;
import digital.asset.manager.application.user.dto.User;
import digital.asset.manager.application.user.repository.UserCacheRepository;
import digital.asset.manager.application.user.repository.UserRefreshTokenRepository;
import digital.asset.manager.application.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserCacheRepository userCacheRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final EmailCacheRepository emailCacheRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder encoder;
    private final S3uploader s3uploader;
    private final ArticleLikeRepository articleLikeRepository;
    private final ConstellationLikeRepository constellationLikeRepository;
    private final ArticleRepository articleRepository;
    private final ConstellationUserRepository constellationUserRepository;
    private final ArticleService articleService;
    private final ConstellationService constellationService;
    private final FollowService followService;

    private final AuthTokenProvider tokenProvider;
    private final AppProperties appProperties;

    @Value("${mail.auth-code-expired-ms}")
    private Long mailExpiredMs;
    private final static long THREE_DAYS_MSEC = 259200000;
    private final static String REFRESH_TOKEN = "refresh_token";
    private final ImageRepository imageRepository;

    public Optional<User> loadUserByEmail(String email) {
        return Optional.ofNullable(userCacheRepository.getUser(email)
                .orElseGet(() ->
                        userRepository.findByEmail(email)
                                .map(User::fromEntity)
                                .orElseThrow(() ->
                                        new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
                                )
                )
        );
    }

    // 이메일 인증 요청 시도
    @Transactional
    public void sendCodeByEmail(String email) {
        validateEmailPattern(email);
        checkEmailExistenceOrException(email);
        String authCode = generateMailCode();  // 코드 생성

        emailService.sendEmail(email, authCode);  // 메일 보내기
        emailCacheRepository.setEmailCode(email, authCode, mailExpiredMs);  // 레디스에 저장
    }

    // 이메일 인증코드 검증
    public boolean verifyEmailCode(String email, String userAuthCode) {
        validateEmailPattern(email);
        checkEmailExistenceOrException(email);
        String authCode = emailCacheRepository.getEmailCode(email).orElseThrow(() -> new ApplicationException(ErrorCode.NEVER_ATTEMPT_EMAIL_AUTH));

        if (!userAuthCode.equals(authCode)) {
            throw new ApplicationException(ErrorCode.INVALID_EMAIL_CODE);
        }
        return true;
    }


    // 이메일 중복 체크(참일 경우 중복되는 이메일 없음)
    public boolean checkDuplicateEmail(String email) {
        return !userRepository.existsByEmail(email);
    }

    // 닉네임 중복 체크(참일 경우 중복되는 닉네임 없음)
    public boolean checkDuplicateNickname(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    /**
     * 로그인
     * 1. JWT 토큰 생성
     * 2. 리프레시 토큰 생성
     * 3. 유저 - 리프레시 토큰이 있는지 확인
     * 4. 없으면 새로 등록
     * 5. 있으면 DB에 업데이트
     * 6. 기존 쿠키 삭제하고 새로 추가
     * 7. JWT 토큰 리턴
     */
    public UserLoginResponse login(HttpServletRequest request, HttpServletResponse response, String email, String password) {
        // 회원가입 여부 체크
        User user = loadUserByEmail(email).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
        );
        userCacheRepository.setUser(user);
        // 비밀번호 체크
        if (!encoder.matches(password, user.password())) {
            throw new ApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        AuthToken accessToken = tokenProvider.createAuthToken(email, user.roleType().getCode(), appProperties.getAuth().getTokenExpiry());
        long refreshTokenExpiry = appProperties.getAuth().getRefreshTokenExpiry();
        AuthToken refreshToken = tokenProvider.createAuthToken(appProperties.getAuth().getTokenSecret(), refreshTokenExpiry);

        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserId(email);
        if (userRefreshToken == null) { // 토큰이 없는 경우. 새로 등록
            userRefreshToken = new UserRefreshToken(email, refreshToken.getToken());
            userRefreshTokenRepository.saveAndFlush(userRefreshToken);
        } else {  // 토큰 발견 -> 리프레시 토큰 업데이트
            userRefreshToken.setRefreshToken(refreshToken.getToken());
        }

        int cookieMaxAge = (int) refreshTokenExpiry / 60;
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
        CookieUtils.addCookie(response, REFRESH_TOKEN, refreshToken.getToken(), cookieMaxAge);
        UserDefaultResponse defaultResponse = UserDefaultResponse.fromUser(user, articleService.countArticles(email), constellationService.countConstellations(email), followService.countFollowers(user.nickname()), followService.countFollowings(user.nickname()));

        return new UserLoginResponse(defaultResponse, accessToken.getToken());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, String email) {
        //레디스에서 삭제
        userCacheRepository.deleteUser(email);
        // 리프레시 토큰 정보 삭제
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserId(email);
        userRefreshTokenRepository.delete(userRefreshToken);

        // 헤더 토큰 삭제
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
    }

    /**
     * 리프레시 토큰
     * 1. 액세스 토큰 기존 헤더에서 가져오기
     * 2. 액세스 토큰 (String)-> (Token)으로 변환
     * 3. 유효한 토큰인지 검증
     * 4. 만료된 토큰인지 검증
     * 5. claims에서 이메일 가져오기
     * 6. Claims에서 role타입 가져오기
     * 7. 프론트로 부터 쿠키에서 리프레시 토큰 가져오기
     * 8. 유효한지 확인
     * 9. 유저-리프레시 레포에서 토큰 가져오기 - 없을 경우 에러 - 로그인할 때 만들어지기 때문에 없을리 없음
     * 10. 새로운 액세스 토큰 발급
     * 11. 리프레시 토큰이 3일 이하로 남았을 경우 리프레시 토큰 갱신
     * 12. DB에 업데이트
     * 13. 액세스 토큰 리턴
     */
    public UserLoginResponse refreshToken(String email, HttpServletRequest request, HttpServletResponse response) {
        // 1. 헤더로 부터 액세스 토큰 가져오기
        String accessToken = HeaderUtils.getAccessToken(request);
        AuthToken authToken = tokenProvider.convertAuthToken(accessToken);
        User user = loadUserByEmail(email).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", email))
        );
        UserDefaultResponse defaultResponse = UserDefaultResponse.fromUser(user, articleService.countArticles(email), constellationService.countConstellations(email), followService.countFollowers(user.nickname()), followService.countFollowings(user.nickname()));

        // 2-1. 토큰이 유효한지 체크
        if (authToken.validate()) {   // 유효하다면 지금 토큰 그대로 반환
            return new UserLoginResponse(defaultResponse, authToken.getToken());
        }

        // 2-2. 토큰이 유효하지 않다면 리프레시 토큰이 있는지 확인하자
        Claims claims = authToken.getExpiredClaims();  // 만료되었을 경우 만료 토큰을 가져옴.
        if (claims == null) {
            return new UserLoginResponse(defaultResponse, accessToken);  // 아직 만료 안됨
        }

        RoleType roleType = RoleType.of(claims.get("role", String.class));

        //refresh token
        String refreshToken = CookieUtils.getCookie(request, REFRESH_TOKEN)
                .map(Cookie::getValue)
                .orElse(null);
        AuthToken authRefreshToken = tokenProvider.convertAuthToken(refreshToken);

        if (!authRefreshToken.validate()) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB 확인
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findByUserIdAndRefreshToken(email, refreshToken);
        if (userRefreshToken == null) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Date now = new Date();
        AuthToken newAccessToken = tokenProvider.createAuthToken(email, roleType.getCode(), appProperties.getAuth().getTokenExpiry());

        long validTime = authRefreshToken.extractClaims().getExpiration().getTime() - now.getTime();

        //refresh 토큰 기간이 3일 이하일 경우 새로 갱신
        if (validTime <= THREE_DAYS_MSEC) {
            authRefreshToken = tokenProvider.createAuthToken(
                    appProperties.getAuth().getTokenSecret(),
                    appProperties.getAuth().getRefreshTokenExpiry()
            );
            userRefreshToken.setRefreshToken(authRefreshToken.getToken());

            int cookieMaxAge = (int) appProperties.getAuth().getRefreshTokenExpiry() / 60;
            CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
            CookieUtils.addCookie(response, REFRESH_TOKEN, authRefreshToken.getToken(), cookieMaxAge);
        }


        return new UserLoginResponse(defaultResponse, newAccessToken.getToken());
    }

    // 회원가입 - 이메일 인증
    @Transactional
    public User join(String email, String password, String name, String nickname) {
        validateEmailPattern(email);
        checkEmailExistenceOrException(email);
        if (satisfyNickname(nickname)) {
            UserEntity userEntity = UserEntity.of(email, ProviderType.LOCAL, encoder.encode(password), name, nickname, null);
            return User.fromEntity(userRepository.save(userEntity));
        }
        throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 회원가입 - 소셜로그인
    @Transactional
    public User join(String email, ProviderType providerType, String password, String name, String nickname) {
        checkEmailExistenceOrException(email);
        if (satisfyNickname(nickname)) {
            UserEntity userEntity = UserEntity.of(email, providerType, password, name, nickname);
            return User.fromEntity(userRepository.save(userEntity));
        }
        throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    //회원정보 조회
    @Transactional(readOnly = true)
    public UserProfileResponse my(String nickName) {
        UserEntity userEntity = userRepository.findByNickname(nickName).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", nickName)));
        User user = User.fromEntity(userEntity);
        return UserProfileResponse.fromUser(
                user,
                articleService.countArticles(user.email()),
                constellationService.countConstellations(user.email()),
                followService.countFollowers(nickName),
                followService.countFollowings(nickName)
        );
    }

    //회원정보 수정
    @Transactional
    public UserProfileResponse updateMyProfile(String email, UserModifyRequest request) {

        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() ->
                new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", email)));
        //닉네임 중복체크
        if (!userEntity.getNickname().equals(request.nickname())) {
            satisfyNickname(request.nickname());
            userEntity.setNickname(request.nickname());
        }
        if (request.password() != null) {
            userEntity.setPassword(encoder.encode(request.password()));
        }
        if (request.name() != null) {
            userEntity.setName(request.name());
        }
        if (request.birthday() != null) {
            userEntity.setBirthday(request.birthday());
        }
        if (request.disclosureType() != null) {
            userEntity.setDisclosureType(request.disclosureType());
        }
        //null이어도 되는 필드
        userEntity.setMemo(request.memo());
        userCacheRepository.updateUser(User.fromEntity(userEntity));
        userRepository.saveAndFlush(userEntity);

        User user = User.fromEntity(userEntity);
        return UserProfileResponse.fromUser(
                user,
                articleService.countArticles(email),
                constellationService.countConstellations(email),
                followService.countFollowers(user.nickname()),
                followService.countFollowings(user.nickname()));
    }

    // 기본 프로필로 변경하기
    @Transactional
    public UserDefaultResponse updateProfileDefault(String email) {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(
                () -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not found", email))
        );
        ImageEntity oldImage = userEntity.getImageEntity();
        if (oldImage == null) { // 이미 기본 프로필일 경우
            throw new ApplicationException(ErrorCode.ALREADY_DEFAULT_IMAGE);
        } else { // 기본 프로필로 변경하는 경우
            s3uploader.deleteImageFromS3(oldImage.getUrl());
            imageRepository.delete(oldImage);
            userEntity.setImageEntity(null);
        }
        User user = User.fromEntity(userEntity);
        return UserDefaultResponse.fromUser(
                user,
                articleService.countArticles(email),
                constellationService.countConstellations(email),
                followService.countFollowers(user.nickname()),
                followService.countFollowings(user.nickname()));
    }

    /**
     * 1. user의 entity를 가져온다.
     * 2. S3에 수정한 profile 사진을 저장한다.
     * 3. userEntity의 id를 가지고 imageService의 getImageUrl 함수를 이용하여 Image Dto를 가져온다.
     * 4. image Table에 수정한 이미지 정보를 저장한다.
     * 5. image Table에서 가져왔던 Image Dto를 이용해 해당하는 부분을 삭제한다.
     * 6. Image Dto에 저장되어있던 url을 이용하여 S3에서 해당하는 이미지 경로를 삭제한다.
     */
    @Transactional
    public UserDefaultResponse updateProfileImage(String email, MultipartFile multipartFile, ImageType imageType) {
        String profileUrl = "";
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(
                () -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not found", email))
        );
        try {
            // 기존의 이미지 불러오기
            ImageEntity oldImgEntity = userEntity.getImageEntity();

            // 1. 기존 이미지가 없을 경우
            if (oldImgEntity == null) {
                profileUrl = s3uploader.uploadProfile(multipartFile, "profiles");
                ImageEntity newImage = ImageEntity.of(multipartFile.getOriginalFilename(), profileUrl, imageType);
                userEntity.setImageEntity(newImage);
                imageRepository.save(newImage);
            }
            // 2. 기존 이미지가 있을 경우
            else {
                //S3에서 기존 이미지 삭제
                s3uploader.deleteImageFromS3(oldImgEntity.getUrl());

                // 새로운 이미지 추가
                profileUrl = s3uploader.uploadProfile(multipartFile, "profiles");
                oldImgEntity.setName(multipartFile.getOriginalFilename());
                oldImgEntity.setUrl(profileUrl);
                oldImgEntity.setImageType(imageType);
                imageRepository.save(oldImgEntity);
            }
            User user = User.fromEntity(userEntity);
            return UserDefaultResponse.fromUser(
                    user,
                    articleService.countArticles(email),
                    constellationService.countConstellations(email),
                    followService.countFollowers(user.nickname()),
                    followService.countFollowings(user.nickname()));
        } catch (IOException e) {
            s3uploader.deleteImageFromS3(profileUrl);
        }
        throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    //회원 탈퇴
    @Transactional
    public void delete(String email) {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", email)));

        articleRepository.findAllByOwnerEntity(userEntity).forEach(articleLikeRepository::deleteAllByArticleEntity);
        constellationUserRepository.findByUserEntityAndConstellationUserRole(userEntity, ConstellationUserRole.ADMIN)
                .forEach(entity -> constellationLikeRepository.deleteAllByConstellationEntity(entity.getConstellationEntity()));
        articleLikeRepository.deleteAllByUserEntity(userEntity);
        constellationLikeRepository.deleteAllByUserEntity(userEntity);
        userRepository.delete(userEntity);
    }

    //이메일 인증코드 생성
    private String generateMailCode() {
        int length = 6;
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("이메일 인증 코드 생성 중 에러 발생 : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean satisfyNickname(String nickname) {
        validateNicknamePattern(nickname);

        userRepository.findByNickname(nickname).ifPresent(it -> {
                    throw new ApplicationException(ErrorCode.DUPLICATED_USER_NICKNAME, String.format("%s is duplcated", nickname));
                }
        );
        return true;
    }

    private void validateEmailPattern(String email) {
        final String REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern regex = Pattern.compile(REGEX);
        Matcher matcher = regex.matcher(email);

        if (!matcher.matches()) {
            throw new ApplicationException(ErrorCode.UNSUITABLE_EMAIL, String.format("%s doesn't meet the conditions", email));
        }
    }

    private void validateNicknamePattern(String nickname) {
        //오직 소문자, 숫자, 4자 이상의 패턴만 가능
        final String REGEX = "^(?=.*[a-z])[a-z0-9_]{4,}$";
        Pattern regex = Pattern.compile(REGEX);
        Matcher matcher = regex.matcher(nickname);

        if (!matcher.matches()) {
            throw new ApplicationException(ErrorCode.UNSUITABLE_NICKNAME, String.format("%s doesn't meet the conditions", nickname));
        }
    }

    // 해당 이메일이 이미 존재하는 이메일인지 확인 체크
    private void checkEmailExistenceOrException(String email) {
        userRepository.findByEmail(email).ifPresent(it -> {
                    throw new ApplicationException(ErrorCode.DUPLICATED_USER_EMAIL, String.format("%s is duplicated", email));
                }
        );
    }

    //좋아요한 게시물 목록 확인
    @Transactional
    public Page<Article> likeArticleList(String email, Pageable pageable) {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", email)));
        return articleLikeRepository.findAllByUserEntityOrderByCreatedAtDesc(userEntity, pageable)
                .map(ArticleLikeEntity::getArticleEntity)
                .map(articleEntity -> getArticle(articleEntity));
    }

    // 닉네임으로 프로필 조회하기
    @Transactional
    public String getProfileImageUrl(String nickname) {
        UserEntity user = userRepository.findByNickname(nickname).orElseThrow(
                () -> new ApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s is not founded", nickname))
        );
        return user.getImageEntity().getUrl();
    }

    public Article getArticle(ArticleEntity entity) {
        Set<String> hashtags = new HashSet<>();
        try{
            hashtags = entity.getArticleHashtagRelationEntities()
                    .stream()
                    .map(ArticleHashtagRelationEntity::getArticleHashtagEntity)
                    .map(ArticleHashtagEntity::getTagName)
                    .collect(Collectors.toSet());
        } catch(NullPointerException e) {
            hashtags = null;
        }

        List<CommentDto> comments = null;
        try {
            comments = entity.getCommentEntities()
                    .stream()
                    .map(CommentDto::from)
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            comments = null;
        }

        Constellation constellation = null;
        try{
            constellation = Constellation.fromEntity(entity.getConstellationEntity());
        } catch(NullPointerException e) {
            constellation = null;
        }

        return new Article(
                entity.getId(),
                entity.getTitle(),
                entity.getHits(),
                entity.getDescription(),
                entity.getDisclosure(),
                hashtags,
                constellation,
                User.fromEntity(entity.getOwnerEntity()),
                comments,
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getDeletedAt(),
                Image.fromEntity(entity.getImageEntity())
        );
    }
}
