package com.whysooman.jpa_study.repository;

import com.whysooman.jpa_study.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    // JPQL 기본 -> 개발자 의존적(직접 쿼리 작성하므로), 컴파일 단계에서 type check 불가능, runtime 단계에서 오류 발생 가능성이 높음
    // JPA 가 지원하는 다양한 쿼리 방법 중 하나
    // JPQL의 문제를 보완하기 위해 나온 문법이 query DSL -> 쿼리를 함수 형태로 제공함, 코드가 길어짐
    @Query("select m from Member m where m.name like concat('%', :keyword, '%')")
    Page<Member> searchByName(String keyword, Pageable pageable);

    // 일반 join 은 실제 질의하는 대상 Entity에 대한 컬럼만 SELECT
    // fetch join ( to-one 은 페이지네이션과 같이 써도 비교적 안전 )
    // fetch join 은 실제 질의하는 대상 Entity 와 Fetch join이 걸려있는 Entity를 포함한 컬럼을 함께 SELECT
    @Query("select m from Member m join fetch m.team")
    List<Member> findAllFetchTeam();

    // EntityGraph: Spring Data JPA가 JPA EntityGraph를 편하게 쓰게 해줌 :contentReference[oaicite:7]{index=7}
    // 페치 조인과 동일함. 직접 join fetch 를 작성하지 않고 해당 어노테이션을 붙여서 편리하게 사용
    // left outer join 만 지원됨
    @EntityGraph(attributePaths = "team")
    @Query("select m from Member m")
    List<Member> findAllWithTeamGraph();

    // 1) Pageable + EntityGraph (to-one 로딩 최적화)
    @EntityGraph(attributePaths = "team")
    Page<Member> findAll(Pageable pageable);

    // 2) Pageable + fetch join(to-one) (countQuery 분리 권장)
    // Spring Data는 복잡한 쿼리의 pagination을 위해 countQuery가 필요할 수 있음 :contentReference[oaicite:3]{index=3}
    @Query(
            value = "select m from Member m join fetch m.team",
            countQuery = "select count(m) from Member m"
    )
    Page<Member> findPageFetchTeam(Pageable pageable);
}
