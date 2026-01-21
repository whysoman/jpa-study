package com.whysooman.jpa_study.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.Objects;

@Getter
@Entity
@Table(
        name = "members",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email")
)
public class Member {

    /*
        GenerationType.SEQUENCE
        - Mysql은 지원 X, 오라클 같은 DB에서 사용되며 Sequence Object 사용함, DB에 Sequence 미리 생성 필요
        - 필요할때마다 DB에서 조회하므로 성능 저하 가능성
        - 이를 해결하기 위해 allocationSize(앨러케이션 사이즈) 옵션이 있음 기본값 50이며 Sequence 콜하게되면 DB에 한번에 50 더해두고 메모리에서 1개씩 차감하면서 쓰는 방식
        - 애플리케이션 내려갈 경우(메모리 초기화 될 경우) 휘발되므로 중간 값이 빌 수 있음

        GenerationType.TABLE
        - Sequence 지원하지 않는 DB에서 비슷하게 사용하기 위해 사용
        - Sequence 관리 테이블 만들어서 각 테이블의 Sequence를 관리하는 방식으로 통합 Sequence 테이블 혹은 각 테이블별 Sequence 테이블을 만들 수 있음
        - Sequence 테이블 조회 및 업데이트가 필요하므로 비효율적임

        GenerationType.AUTO
        - hibernate가 자동으로 전략 선택하도록 위임함
        - 의도치 않은 동작이 발생할 수 있어 권장 X

        GenerationType.UUID
        - 문자열 기반 128비트의 숫자로 구성, 유일성 보장
     */

    // strategy -> 스트래터지(전략)
    @Id // 기본 키 선언
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 생성(자동 생성), IDENTITY -> DB에 위임,
    private Long id;

    // nullable -> null 가능 여부 설정
    @Column(nullable = false, length = 20)
    private String name;

    // 이메일은 유니크 키이면서 불변임
    @Column(nullable = false, length = 20)
    private String email;

    /*
        OneToOne -> 1:1
        OneToMany -> 1:N
        ManyToOne -> N:1
        ManyToMany -> N:M

        같은 팀을 가진 여러 멤버 있음
        여기서는 N:1 이지만
        Team에서는 멤버가 1:N 관계임

        fetch -> 연관관계에 있는 엔티티의 정보를 언제 조회할지에 대한 옵션
        FetchType.LAZY -> 해당 엔티티 즉, Team 에 접근할때 DB 조회
        FetchType.EAGER -> 해당 엔티티 조회 여부 상관없이 쿼리가 발생함

        두 옵션 상관없이 단건 조회가 아닐 경우 N+1 문제 발생할 수 있음
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    protected Member() {}

    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void changeName(String name) { this.name = name; }

    void setTeam(Team team) { this.team = team; }

    // natural-id 기반 equals/hashCode (Hibernate 가이드 권장 방향) :contentReference[oaicite:10]{index=10}
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member other)) return false;
        return Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
