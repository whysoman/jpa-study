package com.whysooman.jpa_study.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "teams")
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    /*
        cascade(캐스케이드)
        - 영속성 전이
        - 부모 엔티티가 영속화될 때 자식 엔티티도 같이 영속화되고,
          부모 엔티티가 삭제될 때 자식 엔티티도 삭제되는 등 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속 상태로 전이되는 것을 의미
        CascadeType.ALL -> 모든 cascade 적용
        CascadeType.PERSIST(퍼시스트) -> 엔티티 영속화 시, 연관된 하위 엔티티도 함께 유지
        CascadeType.MERGE -> 엔티티 상태를 병합할 때, 연관된 하위 엔티티도 모두 병합
        CascadeType.REMOVE -> 엔티티를 제거할 때, 연관된 하위 엔티티도 모두 제거
        CascadeType.DETACH(디태치)
        -> 영속성 컨텍스트에서 엔티티 제거
        -> 부모 엔티티를 detach() 수행하면, 연관 하위 엔티티도 detach() 상태가 되어 변경사항 반영 X
        CascadeType.REFRESH -> 상위 엔티티를 새로고침 할 때, 연관된 하위 엔티티도 모두 새로고침

        orphanRemoval(오어펀 리무벌)
        - 고아 제거 여부
        - true 일 경우 부모 엔티티 삭제 시 연관된 하위 엔티티도 모두 삭제됨
        - 부모와 자식 엔티티 관계를 제거하면 자식은 고아로 취급되어 사라짐
        - 반면 CascadeType.REMOVE의 경우 관계 제거 시 자식은 그대로 남아 있음
     */
    @OneToMany(mappedBy = "team",
                cascade = CascadeType.PERSIST,
                orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    protected Team() {}

    public Team(String name) { this.name = name; }

    public void addMember(Member member) {
        members.add(member);
        member.setTeam(this);   // 양방향 동기화
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setTeam(null);
    }
}
