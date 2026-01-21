package com.whysooman.jpa_study.repository;

import com.whysooman.jpa_study.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * to-many fetch join + pagination은 데이터 뻥튀기/메모리 페이징 등 문제가 생기기 쉬움.
     * 그래서 2-step(IDs paging -> in 조회 fetch) 패턴을 실습할 예정.
     * (실무에서도 흔한 접근)
     */
    @Query("select t.id from Team t order by t.id")
    Page<Long> findIds(Pageable pageable);

    // Slice 는 Page 와 달리 count 쿼리를 안 날리는 장점이 있어 2-step paging 에 잘 맞음
    @Query("select t.id from Team t order by t.id")
    Slice<Long> findIdSlice(Pageable pageable);

    @Query("""
      select distinct t
      from Team t
      left join fetch t.members
      where t.id in :ids
      order by t.id
    """)
    List<Team> findWithMembersByIdIn(@Param("ids") List<Long> ids);
}
