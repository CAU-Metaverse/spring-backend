package CAUmetaverse.CAUmetaverse.member;


import org.springframework.security.crypto.password.PasswordEncoder;

import CAUmetaverse.CAUmetaverse.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "MEMBER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id; //primary Key

    @Column(nullable = false, length = 30, unique = true)
    private String username;//ID

    private String password;//PW

    @Column(nullable = false, length = 30)
    private String name;//realName

    @Column(nullable = false, length = 30)
    private String nickName;//nickName

    @Column(nullable = false, length = 30)
    private Integer classOf; // hak bun : 19hak_bun, 18hakbun,,,

    @Enumerated(EnumType.STRING)
    private Role role;//USERTYPE -> USER, ADMIN

    @Column(length = 1000)
    private String refreshToken;



    //== modify ==//
    // some might be not going to use
    public void updatePassword(PasswordEncoder passwordEncoder, String password){
        this.password = passwordEncoder.encode(password);
    }

    public void updateName(String name){
        this.name = name;
    }

    public void updateNickName(String nickName){
        this.nickName = nickName;
    }
    
    public void updateRefreshToken(String refreshToken) {
    	this.refreshToken = refreshToken;
    }
    
    public void destroyRefreshToken() {
    	this.refreshToken = null;
    }

    //== encrypt PW ==//
    public void encodePassword(PasswordEncoder passwordEncoder){
        this.password = passwordEncoder.encode(password);
    }

} 