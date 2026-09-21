package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.comment.infra.dto.CommentResponse;
import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.festival.dto.FestivalBrowseResponse;
import com.team007.room_escape.domain.festival.controller.FestivalBrowseController;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalSubmissionRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.domain.post.controller.PostController;
import com.team007.room_escape.domain.post.service.PostService;
import com.team007.room_escape.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DataJpaTest(properties = {
        "spring.config.location=optional:classpath:/community-test.properties",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({FestivalBrowseService.class, PostService.class})
class CommunityBrowseTest {
    @Autowired EntityManager em;
    @Autowired FestivalBrowseService browse;
    @Autowired PostService posts;
    @Autowired FestivalSubmissionRepository submissions;

    private Member member(String nickname) {
        var member = Member.builder().email(UUID.randomUUID() + "@example.com")
                .password("unused-test-password").nickname(nickname).role(MemberRole.ROLE_NORMAL).build();
        em.persist(member);
        return member;
    }

    private Festival festival(Member member, String title, LocalDateTime end) {
        var festival = Festival.builder().member(member).title(title).content("공개 행사 소개")
                .providerType(member == null ? ProviderType.PUBLIC : ProviderType.MEMBER)
                .beginDe(LocalDateTime.now().minusDays(2)).endDe(end).build();
        em.persist(festival);
        return festival;
    }

    private FestivalSubmission submit(Festival festival) {
        var submission = FestivalSubmission.builder().festival(festival).content("비공개 제보 사유").build();
        em.persist(submission);
        return submission;
    }

    private Post post(Member member, Festival festival, String title) {
        var post = Post.builder().member(member).festival(festival).title(title).content("후기 내용").build();
        em.persist(post);
        return post;
    }

    @Test
    void sharedListIncludesOtherOwnersButExcludesDeletedAndPublicItemsWithoutDuplicatingEvents() {
        var me = member("나");
        var neighbor = member("이웃");
        var own = festival(me, "A 내 행사", null);
        submit(own);
        var other = festival(neighbor, "B 이웃 행사", null);
        var otherSubmission = submit(other);
        submit(other);
        festival(null, "공공 행사", null);
        var withdrawn = festival(neighbor, "철회된 제보", null);
        submit(withdrawn).delete();
        var deleted = festival(neighbor, "삭제된 행사", null);
        submit(deleted);
        deleted.delete();
        em.flush();

        var first = browse.submissions("", null, PageRequest.of(0, 1, Sort.by("title")));
        var second = browse.submissions("", null, PageRequest.of(1, 1, Sort.by("title")));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).extracting(FestivalBrowseResponse::festivalId).containsExactly(own.getId());
        assertThat(second.getContent()).extracting(r -> r.submitter().nickname()).containsExactly("이웃");
        assertThat(submissions.findByIdAndFestival_Member_IdAndDeletedAtIsNull(otherSubmission.getId(), me.getId())).isEmpty();
    }

    @Test
    void searchEscapesWildcardsAndUsesDatesForOpenAndClosedFilters() {
        var neighbor = member("이웃");
        submit(festival(neighbor, "100% 만족 행사", null));
        submit(festival(neighbor, "100점 행사", LocalDateTime.now().plusDays(1)));
        submit(festival(neighbor, "종료된 행사", LocalDateTime.now().minusDays(1)));
        var page = PageRequest.of(0, 10);
        assertThat(browse.submissions("100%", null, page).getContent())
                .extracting(FestivalBrowseResponse::title).containsExactly("100% 만족 행사");
        assertThat(browse.submissions("", FestivalStatus.OPEN, page).getTotalElements()).isEqualTo(2);
        assertThat(browse.submissions("", FestivalStatus.CLOSED, page).getContent())
                .extracting(FestivalBrowseResponse::title).containsExactly("종료된 행사");
    }

    @Test
    void publicDetailsOmitPrivateSubmissionFieldsAndWithdrawnItemsCannotBeOpened() {
        var neighbor = member("이웃");
        var shared = festival(neighbor, "이웃 행사", null);
        var submission = submit(shared);
        var publicEvent = festival(null, "문화행사", null);
        assertThat(browse.detail(shared.getId()).festivalContent()).isEqualTo("공개 행사 소개");
        assertThat(browse.detail(publicEvent.getId()).submitter()).isNull();
        assertThat(Arrays.stream(FestivalBrowseResponse.class.getRecordComponents()).map(r -> r.getName()))
                .doesNotContain("submissionContent", "email", "password", "phone");
        assertThat(Arrays.stream(FestivalBrowseResponse.Submitter.class.getRecordComponents()).map(r -> r.getName()))
                .containsExactly("id", "nickname");
        submission.delete();
        em.flush();
        assertThatThrownBy(() -> browse.detail(shared.getId())).isInstanceOf(BusinessException.class);
        publicEvent.delete();
        em.flush();
        assertThatThrownBy(() -> browse.detail(publicEvent.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    void globalReviewFeedPaginatesAcrossEventsAndExcludesDeletedPostsAndEvents() {
        var writer = member("작성자");
        var first = festival(null, "첫 행사", null);
        var second = festival(null, "다른 행사", null);
        post(writer, first, "A 후기");
        post(writer, second, "B 후기");
        post(writer, first, "삭제한 후기").delete();
        var removed = festival(null, "삭제한 행사", null);
        post(writer, removed, "행사가 삭제된 후기");
        removed.delete();
        em.flush();
        var page = posts.getAllPosts(PageRequest.of(0, 1, Sort.by("title")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(r -> r.festivalId()).containsExactly(first.getId());
        assertThat(posts.getAllPosts(PageRequest.of(1, 1, Sort.by("title"))).getContent())
                .extracting(r -> r.festivalId()).containsExactly(second.getId());
    }

    @Test
    void sharedReadRoutesReturnThePagedContractAndPublicDetails() throws Exception {
        var author = member("다른 회원");
        var event = festival(author, "공유 낭독회", null);
        submit(event);
        post(author, event, "낭독회 후기");
        em.flush();
        var mvc = MockMvcBuilders.standaloneSetup(
                new FestivalBrowseController(browse), new PostController(posts))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
        mvc.perform(get("/api/v1/posts").param("page", "0").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].festivalId").value(event.getId()));
        mvc.perform(get("/api/v1/festivals/submissions").param("q", "낭독회").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].submitter.nickname").value("다른 회원"))
                .andExpect(jsonPath("$.data.content[0].submissionContent").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].submitter.email").doesNotExist());
        mvc.perform(get("/api/v1/festivals/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerType").value("MEMBER"))
                .andExpect(jsonPath("$.data.festivalContent").value("공개 행사 소개"));
    }

    @Test
    void commentDateRemainsTheCreationTimeAfterEditing() {
        var writer = member("작성자");
        var post = post(writer, festival(null, "행사", null), "후기");
        var comment = Comment.builder().member(writer).post(post).content("원래 댓글").build();
        var created = LocalDateTime.of(2026, 9, 21, 15, 24);
        ReflectionTestUtils.setField(comment, "createdAt", created);
        ReflectionTestUtils.setField(comment, "updatedAt", created.plusHours(1));
        comment.update("수정한 댓글");
        assertThat(CommentResponse.CommentInfo.from(comment).date()).isEqualTo(created);
    }
}
