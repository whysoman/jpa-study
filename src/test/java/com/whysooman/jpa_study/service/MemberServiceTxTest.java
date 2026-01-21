package com.whysooman.jpa_study.service;

import com.whysooman.jpa_study.repository.MemberRepository;
import com.whysooman.jpa_study.support.MariaDbContainerBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class MemberServiceTxTest extends MariaDbContainerBase {

    @Autowired MemberService memberService;
    @Autowired TxFacadeService txFacadeService;
    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("정상 종료 시 커밋된다")
    void commit_on_success() {
        String email = "commit1@example.com";

        memberService.createMember("a", email);

        assertThat(memberRepository.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("RuntimeException이면 롤백된다")
    void rollback_on_runtime_exception() {
        String email = "rb1@example.com";

        // 예외 처리 해줘도 롤백됨
        // unchecked exception 은 예외 처리가 필수가 아니고 예외처리 해줘도 롤백됨
        assertThatThrownBy(() -> memberService.createThenThrowRuntime("a", email))
                .isInstanceOf(IllegalStateException.class);

        assertThat(memberRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("Checked 예외는 기본 정책상 롤백이 아니라 커밋될 수 있다(주의)")
    void checked_exception_commits_by_default() throws Exception {
        // Uncheced Exception -> 예외 처리 필수 아님, 롤백 발생
        // Checked Exception -> 예외 처리 필수로 해야함, 롤백 발생하지 않음(롤백 강제할 수는 있음)
        String email = "chk1@example.com";

        try {
            memberService.createThenThrowChecked("a", email);
        } catch (CheckedBusinessException ignored) {
            System.out.println("Checked Exception 발생!! 롤백 안됨");
        }

        // 기본 롤백 규칙: checked exception은 롤백 대상이 아님 :contentReference[oaicite:3]{index=3}
        assertThat(memberRepository.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("rollbackFor를 주면 Checked 예외도 롤백된다")
    void checked_exception_rolls_back_when_configured() throws Exception {
        String email = "chk2@example.com";

        // rollbackFor 에 롤백하고 싶은 exception 에 대해 정의하면 해당 exception 발생 시 롤백됨
        // 물론 그 외 롤백이 발생하는 exception 에 대해 롤백되는건 동일함
        try {
            memberService.createThenThrowCheckedRollback("a", email);
        } catch (CheckedBusinessException ignored) {
            System.out.println("Checked Exception 발생!! rollbackFor 에 정의하여 롤백 되게함!");
        }

        assertThat(memberRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("REQUIRES_NEW는 outer 롤백과 무관하게 커밋될 수 있다")
    void requires_new_commits_even_if_outer_rolls_back() {
        String auditEmail = "audit@example.com";
        String outerEmail = "outer@example.com";

        assertThatThrownBy(() -> txFacadeService.outerFailsButAuditCommits(auditEmail, outerEmail))
                .isInstanceOf(IllegalStateException.class);

        // audit(REQUIRES_NEW): 커밋 기대
        // propagation(프라퍼게이션) 설정으로 전파 방식 설정
        // @Transactional(propagation = Propagation.REQUIRES_NEW)
        // Propagation.REQUIRES_NEW(리콰이어즈 뉴) -> 항상 새로운 트랜잭션이 필요
        // Propagation.REQUIRED(리콰이어드) -> 기본 옵션. 트랜잭션이 필요하고, 기존 트랜잭션이 있으면 사용하고 없으면 새로 만듬. * 롤백 시 동일한 트랜잭션 모두 롤백됨
        // Propagation.MANDATORY(맨더토리) -> 트랜잭션이 의무임. 메서드 호출 시 반드시 드랜잭션이 설정되어 있어야함(트랜잭션이 없다면 IllegalTransactionStateException 예외 발생), 기존 트랜잭션 내에서 실행된다고 보면됨
        // Propagation.SUPPORTS(서폴츠) -> 기존 트랜잭션이 있으면 사용하고 없으면 트랜잭션 생성 없이 진행(트랜잭션으로 묶이지 않음)
        // Propagation.NOT_SUPPORTED(서폴티드) -> 트랜잭션을 지원하지 않는다는 뜻으로 기존에 트랜잭션이 있어도 트랜잭션 없는 상태로 진행함
        // Propagation.NEVER -> 트랜잭션 지원 X, 상위 스코프에도 트랜잭션이 설정되어 있으면 안됨. 트랜잭션 설정되어 있을 경우 IllegalTransactionStateException 예외 발생
        // Propagation.NESTED(네스티드) -> 중첩 트랜잭션 (자식 트랜잭션)을 만든다. 호출부에서 트랜잭션이 있을 경우 중첩 트랜잭션을 생성하여 실행. 트랜잭션 없는 상태일 경우 새로운 트랜잭션 생성.
        //                                하위 메서드에서 예외 발생 시 하위 메서드(중첩 트랜잭션)만 롤백 상위 트랜잭션이 영향 받지는 않음
        assertThat(memberRepository.findByEmail(auditEmail)).isPresent();
        // outer: 롤백 기대
        assertThat(memberRepository.findByEmail(outerEmail)).isEmpty();
    }
}
