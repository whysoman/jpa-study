package com.whysooman.jpa_study.service;

import com.whysooman.jpa_study.domain.Member;
import com.whysooman.jpa_study.domain.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequiresNewAuditService {

    @PersistenceContext
    private EntityManager em;

    // propagation 설정으로 전파 방식 설정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAuditMember(String email) {
        Team team = new Team("audit-team");
        Member member = new Member("audit", email);
        team.addMember(member);
        em.persist(team);
    }
}
