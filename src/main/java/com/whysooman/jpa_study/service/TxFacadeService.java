package com.whysooman.jpa_study.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TxFacadeService {

    private final MemberService memberService;
    private final RequiresNewAuditService auditService;

    public TxFacadeService(MemberService memberService, RequiresNewAuditService auditService) {
        this.memberService = memberService;
        this.auditService = auditService;
    }

    /**
     * outer 트랜잭션은 롤백되지만,
     * REQUIRES_NEW로 수행한 audit는 커밋되는지 확인.
     */
    @Transactional
    public void outerFailsButAuditCommits(String auditEmail, String outerEmail) {
        auditService.writeAuditMember(auditEmail);                // 별도 트랜잭션(커밋 기대)
        memberService.createMember("outer", outerEmail);    // outer 트랜잭션에 참가(롤백 기대)
        throw new IllegalStateException("outer boom");
    }
}
