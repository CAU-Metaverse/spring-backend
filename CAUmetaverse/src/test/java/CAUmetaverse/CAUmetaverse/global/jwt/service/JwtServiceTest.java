package CAUmetaverse.CAUmetaverse.global.jwt.service;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import CAUmetaverse.CAUmetaverse.member.Member;
import CAUmetaverse.CAUmetaverse.member.Role;
import CAUmetaverse.CAUmetaverse.member.repository.MemberRepository;
import CAUmetaverse.global.jwt.service.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;


@SpringBootTest
@Transactional
class JwtServiceTest {



    @Autowired JwtService jwtService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access.header}")
    private String accessHeader;
    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String USERNAME_CLAIM = "username";
    private static final String BEARER = "Bearer ";


    private String username = "username";

    @BeforeEach
    public void init(){
        Member member = Member.builder().username(username).password("1234567890").name("Member1").nickName("NickName1").role(Role.USER).classOf(22).build();
        memberRepository.save(member);
        clear();
    }

    private void clear(){
        em.flush();
        em.clear();
    }

    private DecodedJWT getVerify(String token) {
        return JWT.require(HMAC512(secret)).build().verify(token);
    }
    
    @Test
    public void createAccessToken_AccessToken() throws Exception {
        //given, when
        String accessToken = jwtService.createAccessToken(username);

        DecodedJWT verify = getVerify(accessToken);

        String subject = verify.getSubject();
        String findUsername = verify.getClaim(USERNAME_CLAIM).asString();

        //then
        assertThat(findUsername).isEqualTo(username);
        assertThat(subject).isEqualTo(ACCESS_TOKEN_SUBJECT);
    }
    
    @Test
    public void createRefreshToken_RefreshToken() throws Exception {
        //given, when
        String refreshToken = jwtService.createRefreshToken();
        DecodedJWT verify = getVerify(refreshToken);
        String subject = verify.getSubject();
        String username = verify.getClaim(USERNAME_CLAIM).asString();

        //then
        assertThat(subject).isEqualTo(REFRESH_TOKEN_SUBJECT);
        assertThat(username).isNull();
    }
    
    @Test
    public void updateRefreshToken_refreshToken() throws Exception {
        //given
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();
        Thread.sleep(3000);

        //when
        String reIssuedRefreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, reIssuedRefreshToken);
        clear();

        //then
        assertThrows(Exception.class, () -> memberRepository.findByRefreshToken(refreshToken).get());//
        assertThat(memberRepository.findByRefreshToken(reIssuedRefreshToken).get().getUsername()).isEqualTo(username);
    }
    
    @Test
    public void destroyRefreshToken_refreshToken() throws Exception {
        //given
        String refreshToken = jwtService.createRefreshToken();
        jwtService.updateRefreshToken(username, refreshToken);
        clear();

        //when
        jwtService.destroyRefreshToken(username);
        clear();

        //then
        assertThrows(Exception.class, () -> memberRepository.findByRefreshToken(refreshToken).get());

        Member member = memberRepository.findByUsername(username).get();
        assertThat(member.getRefreshToken()).isNull();
    }
    
    @Test
    public void setAccessTokenHeader_AccessToken() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.setAccessTokenHeader(mockHttpServletResponse, accessToken);


        //when
        jwtService.sendToken(mockHttpServletResponse,accessToken,refreshToken);

        //then
        String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);

        assertThat(headerAccessToken).isEqualTo(accessToken);
        
    }



    @Test
    public void setRefreshTokenHeader_RefreshToken() throws Exception {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();

        jwtService.setRefreshTokenHeader(mockHttpServletResponse, refreshToken);


        //when
       // jwtService.sendAccessAndRefreshToken(mockHttpServletResponse,accessToken,refreshToken);

        //then
        String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);

        assertThat(headerRefreshToken).isEqualTo(refreshToken);
    }
    
	@Test
	public void sendToken() throws Exception {
		//given
		MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

		String accessToken = jwtService.createAccessToken(username);
		String refreshToken = jwtService.createRefreshToken();


		//when
		jwtService.sendToken(mockHttpServletResponse,accessToken,refreshToken);


		//then
		String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);
		String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);



		assertThat(headerAccessToken).isEqualTo(accessToken);
		assertThat(headerRefreshToken).isEqualTo(refreshToken);

	}
	//accessheader + refreshHead �ؼ� ��ū�� ��� ���ֱ�
	private HttpServletRequest setRequest(String accessToken, String refreshToken) throws IOException {

	    MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
	    jwtService.sendToken(mockHttpServletResponse,accessToken,refreshToken);

	    String headerAccessToken = mockHttpServletResponse.getHeader(accessHeader);
	    String headerRefreshToken = mockHttpServletResponse.getHeader(refreshHeader);

	    MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();

	    httpServletRequest.addHeader(accessHeader, BEARER+headerAccessToken);
	    httpServletRequest.addHeader(refreshHeader, BEARER+headerRefreshToken);

	    return httpServletRequest;
	}
	
    @Test
    public void extractAccessToken_AccessToken() throws Exception {
        //given
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();
        HttpServletRequest httpServletRequest = setRequest(accessToken, refreshToken);

        //when
        String extractAccessToken = jwtService.extractAccessToken(httpServletRequest);


        //then
        assertThat(extractAccessToken).isEqualTo(accessToken);
        assertThat(getVerify(extractAccessToken).getClaim(USERNAME_CLAIM).asString()).isEqualTo(username);
    }
    
    @Test
    public void extractRefreshToken_RefreshToken() throws Exception {
        //given
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();
        HttpServletRequest httpServletRequest = setRequest(accessToken, refreshToken);


        //when
        String extractRefreshToken = jwtService.extractRefreshToken(httpServletRequest);


        //then
        assertThat(extractRefreshToken).isEqualTo(refreshToken);
        assertThat(getVerify(extractRefreshToken).getSubject()).isEqualTo(REFRESH_TOKEN_SUBJECT);
    }
    
    @Test
    public void extractUsername_Username() throws Exception {
        //given
        String accessToken = jwtService.createAccessToken(username);
        String refreshToken = jwtService.createRefreshToken();
        HttpServletRequest httpServletRequest = setRequest(accessToken, refreshToken);

        String requestAccessToken = jwtService.extractAccessToken(httpServletRequest);

        //when
        String extractUsername = jwtService.extractUsername(requestAccessToken);


        //then
        assertThat(extractUsername).isEqualTo(username);
    }
	
    
    
	
}