package com.whysooman.jpa_study.domain;

import com.whysooman.jpa_study.repository.MemberRepository;
import com.whysooman.jpa_study.support.MariaDbContainerBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// DataJpaTest -> JPA 관련 테스트만 로드
// 트랜잭션이 기본임
@DataJpaTest
@ActiveProfiles("test") // profile 환경 지정
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // DataJpaTest 사용 시 기본적으로 내장 DB로 바꿀 수 있어 실제 DB 사용하려고 해당 어노테이션 작성함
public class MemberMappingTest extends MariaDbContainerBase {

    // AutoConfigureTestDatabase.Replace.NONE -> DataSource 를 대체하지 않음
    // AutoConfigureTestDatabase.Replace.AUTO_CONFIGURE -> 필요하다면 자동 구성된 것으로 대체
    // AutoConfigureTestDatabase.Replace.ANY -> 구성과 상관 없이 DataSource 를 대체함

    // Autowired -> 의존성 주입을 자동화 하는 어노테이션임
    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("IDENTITY 전략이면 저장 시점에 ID가 생성된다")
    void identity_id_generated() {
        // 인스턴스(객체) 생성
        // given
        Member member = new Member("chang", "chang@naver.com");

        // 저장 및 저장된 데이터 가져옴
        // when
        Member saved = memberRepository.save(member);

        System.out.println("save 호출 후");

        // 저장한 결과의 id 값이 null이 아닌지 확인
        // then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("email 유니크 제약은 flush 시점에 위반이 확정된다")
    void unique_constraint_violated_on_flush() {
        // given
        memberRepository.save(new Member("a", "dup@example.com"));
        memberRepository.save(new Member("b", "dup@example.com"));

        // 트랜잭션은 작업을 하나의 논리 단위로 묶어 모두 성공하거나 모두 실패하는 것을 보장하는 것임
        // 트랜잭션은 데이터의 일관성과 안정적을 유지하는 개념
        // ACID 속성을 가짐 -> 
        // 원자성(Atomicity) : 트랜잭션은 모두 성공이 아니면 모두 실행되지 않음
        // 일관성(Consistency) : 트랜잭션이 실행되지 전후로 데이터베이스는 항상 일관된 상태 유지
        // 고립성(Isolation) : 여러 트랜잭션이 동시에 실행되더라도 서로 영향을 주지 않고 독립적으로 실행되는 것처럼 보임
        // 지속성(Durability) : 성공적으로 완료된 트랜잭션의 결과는 시스템 장애가 발생해도 영구적으로 보존되어야함

        // flush 강제 호출하여 트랜잭션이 commit 되도록 함
        // assertThatThrownBy -> 에러가 발생해야 하는 상황 테스트 하는 코드
        // .isInstanceOf(DataIntegrityViolationException.class); -> 특정 예외 객체가 데이터 무결성 위반으로 인해 발생한 예외인지 확인하는 데 사용
        // when / then
        assertThatThrownBy(() -> memberRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
