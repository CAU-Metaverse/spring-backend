package CAUmetaverse.member.repository;

import CAUmetaverse.CAUmetaverse.member.Member;
import CAUmetaverse.CAUmetaverse.member.Role;
import CAUmetaverse.CAUmetaverse.member.repository.MemberRepository;
import lombok.Builder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ContextConfiguration(classes = CAUmetaverse.CaUmetaverseApplication.class)
class MemberRepositoryTest {


    @Autowired 
    MemberRepository memberRepository;
    
    @Autowired 
    EntityManager em;



    private void clear(){
        em.flush();
        em.clear();
    }

    @AfterEach
    private void after(){
        em.clear();
    }

    @Test
    public void userMake() throws Exception {
        //given
        Member member = Member.builder().username("username").password("1234567890").name("Member1").nickName("NickName1").role(Role.USER).classOf(22).build();

        //when
        Member saveMember = memberRepository.save(member);

        //then
        Member findMember = memberRepository.findById(saveMember.getId()).orElseThrow(() -> new RuntimeException("����� ȸ���� �����ϴ�"));//���� ���� Ŭ������ ������ �ʾұ⿡ RuntimeException���� ó���ϰڽ��ϴ�.

        assertThat(findMember).isSameAs(saveMember);
        assertThat(findMember).isSameAs(member);
    }

    @Test
    public void error_userMake_withoutID() throws Exception {
        //given
        Member member = Member.builder().password("1234567890").name("Member1").nickName("NickName1").role(Role.USER).classOf(22).build();

        //when, then
        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void error_userMake_noName() throws Exception {
        //given
        Member member = Member.builder().username("username").password("1234567890").nickName("NickName1").role(Role.USER).classOf(22).build();

        //when, then
        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void error_userMake_noNickname() throws Exception {
        //given
        Member member = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).classOf(22).build();

        //when, then
        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void error_userMake_noAge() throws Exception {
        //given
        Member member = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").build();

        //when, then
        assertThrows(Exception.class, () -> memberRepository.save(member));
    }

    @Test
    public void error_userMake_idAlreadyExists() throws Exception {
        //given
        Member member1 = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        Member member2 = Member.builder().username("username").password("1111111111").name("Member2").role(Role.USER).nickName("NickName2").classOf(22).build();

        memberRepository.save(member1);
        clear();

        //when, then
        assertThrows(Exception.class, () -> memberRepository.save(member2));

    }

    @Test
    public void userModify() throws Exception {
        //given
        Member member1 = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        memberRepository.save(member1);
        clear();

        String updatePassword = "updatePassword";
        String updateName = "updateName";
        String updateNickName = "updateNickName";

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        //when
        Member findMember = memberRepository.findById(member1.getId()).orElseThrow(() -> new Exception());
        findMember.updateName(updateName);
        findMember.updateNickName(updateNickName);
        findMember.updatePassword(passwordEncoder,updatePassword);
        em.flush();

        //then
        Member findUpdateMember = memberRepository.findById(findMember.getId()).orElseThrow(() -> new Exception());

        assertThat(findUpdateMember).isSameAs(findMember);
        assertThat(passwordEncoder.matches(updatePassword, findUpdateMember.getPassword())).isTrue();
        assertThat(findUpdateMember.getName()).isEqualTo(updateName);
        assertThat(findUpdateMember.getName()).isNotEqualTo(member1.getName());
    }

    @Test
    public void userDelete() throws Exception {
        //given
        Member member1 = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        memberRepository.save(member1);
        clear();

        //when
        memberRepository.delete(member1);
        clear();

        //then
        assertThrows(Exception.class,
                () -> memberRepository.findById(member1.getId())
                        .orElseThrow(() -> new Exception()));
    }


    @Test
    public void existByUsername_test() throws Exception {
        //given
        String username = "username";
        Member member1 = Member.builder().username(username).password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        memberRepository.save(member1);
        clear();

        //when, then
        assertThat(memberRepository.existsByUsername(username)).isTrue();
        assertThat(memberRepository.existsByUsername(username+"123")).isFalse();

    }


    @Test
    public void findByUsername_test() throws Exception {
        //given
        String username = "username";
        Member member1 = Member.builder().username(username).password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        memberRepository.save(member1);
        clear();


        //when, then
        assertThat(memberRepository.findByUsername(username).get().getUsername()).isEqualTo(member1.getUsername());
        assertThat(memberRepository.findByUsername(username).get().getName()).isEqualTo(member1.getName());
        assertThat(memberRepository.findByUsername(username).get().getId()).isEqualTo(member1.getId());
        assertThrows(Exception.class,
                () -> memberRepository.findByUsername(username+"123")
                        .orElseThrow(() -> new Exception()));

    }

    @Test
    public void userMake_timeCheck() throws Exception {
        //given
        Member member1 = Member.builder().username("username").password("1234567890").name("Member1").role(Role.USER).nickName("NickName1").classOf(22).build();
        memberRepository.save(member1);
        clear();

        //when
        Member findMember = memberRepository.findById(member1.getId()).orElseThrow(() -> new Exception());

        //then
        assertThat(findMember.getCreatedDate()).isNotNull();
        assertThat(findMember.getLastModifiedDate()).isNotNull();

    }


}