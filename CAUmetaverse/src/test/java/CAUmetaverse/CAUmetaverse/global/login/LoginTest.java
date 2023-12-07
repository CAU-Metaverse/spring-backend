package CAUmetaverse.CAUmetaverse.global.login;

import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import CAUmetaverse.CAUmetaverse.member.Member;
import CAUmetaverse.CAUmetaverse.member.Role;
import CAUmetaverse.CAUmetaverse.member.repository.MemberRepository;


@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class LoginTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	EntityManager em;

	PasswordEncoder delegatingPasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	ObjectMapper objectMapper = new ObjectMapper();

	private static String KEY_USERNAME = "username";
	private static String KEY_PASSWORD = "password";
	private static String USERNAME = "username";
	private static String PASSWORD = "123456789";

	private static String LOGIN_URL = "/login";


	private void clear() {
		em.flush();
		em.clear();
	}

	@BeforeEach
	private void init() {
		memberRepository.save(Member.builder()
			.username(USERNAME)
			.password(delegatingPasswordEncoder.encode(PASSWORD))
			.name("Member1")
			.nickName("NickName1")
			.role(Role.USER)
			.classOf(22)
			.build());
		clear();
	}

	private Map getUsernamePasswordMap(String username, String password) {
		Map<String, String> map = new HashMap<>();
		map.put(KEY_USERNAME, username);
		map.put(KEY_PASSWORD, password);
		return map;
	}

	private ResultActions perform(String url, MediaType mediaType, Map usernamePasswordMap) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders
			.post(url)
			.contentType(mediaType)
			.content(objectMapper.writeValueAsString(usernamePasswordMap)));

	}
	@Test
	public void loginSuccess() throws Exception {
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);


		//when
		MvcResult result = perform(LOGIN_URL, APPLICATION_JSON, map)
			.andDo(print())
			.andExpect(status().isOk())
			.andReturn();
	}



	@Test
	public void loginFail_wrongID() throws Exception {
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME+"123", PASSWORD);

		//when
		MvcResult result = perform(LOGIN_URL, APPLICATION_JSON, map)
			.andDo(print())
			.andExpect(status().isOk())
			.andReturn();
	}




	@Test
	public void loginFail_wrongPW() throws Exception {
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD+"123");


		//when
		MvcResult result = perform(LOGIN_URL, APPLICATION_JSON, map)
			.andDo(print())
			.andExpect(status().isOk())
			.andReturn();
	}


	@Test
	public void wrongLoginAddr_FORBIDDEN() throws Exception {
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);


		//when, then
		perform(LOGIN_URL+"123", APPLICATION_JSON, map)
			.andDo(print())
			.andExpect(status().isForbidden());

	}


	@Test
	public void login_notJSON_400() throws Exception {
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);

		//when, then
		perform(LOGIN_URL, APPLICATION_FORM_URLENCODED, map)
			.andDo(print())
			.andExpect(status().isOk())
			.andReturn();
	}

	@Test
	public void tryLogin_GET_NOTFOUND() throws Exception {
		// 로그인 시 METHOD가 GET이면 NOTFOUND
		//given
		Map<String, String> map = getUsernamePasswordMap(USERNAME, PASSWORD);


		//when
		mockMvc.perform(MockMvcRequestBuilders
				.get(LOGIN_URL)
				.contentType(APPLICATION_FORM_URLENCODED)
				.content(objectMapper.writeValueAsString(map)))
			.andDo(print())
			.andExpect(status().isNotFound());
	}

}