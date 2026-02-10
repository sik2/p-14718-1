package com.back.boundedContext.post.in;

import com.back.boundedContext.post.app.PostFacade;
import com.back.boundedContext.post.domain.Post;
import com.back.boundedContext.post.domain.PostMember;
import com.back.global.rsData.RsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
@Profile("!prod")
public class PostDataInit {
    private static final int WAIT_SECONDS = 30;
    private static final int RETRY_INTERVAL_MS = 1000;

    private final PostDataInit self;
    private final PostFacade postFacade;

    public PostDataInit(
            @Lazy PostDataInit self,
            PostFacade postFacade
    ) {
        this.self = self;
        this.postFacade = postFacade;
    }

    @Bean
    public ApplicationRunner postDataInitApplicationRunner() {
        return args -> {
            if (waitForMemberSync()) {
                self.makeBasePosts();
                self.makeBasePostComments();
            }
        };
    }

    private boolean waitForMemberSync() {
        log.info("Waiting up to {}s for member sync...", WAIT_SECONDS);

        int maxRetries = WAIT_SECONDS * 1000 / RETRY_INTERVAL_MS;
        for (int i = 0; i < maxRetries; i++) {
            if (postFacade.findMemberByUsername("user1").isPresent()) {
                log.info("Member sync completed. Proceeding with data init.");
                return true;
            }
            if (i > 0 && i % 5 == 0) {
                log.info("Still waiting for member sync... {}s elapsed", i);
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Wait interrupted.");
                return false;
            }
        }

        log.warn("Member sync timeout after {}s. Skipping data init. (Kafka event not received?)", WAIT_SECONDS);
        return false;
    }

    @Transactional
    public void makeBasePosts() {
        if (postFacade.count() > 0) return;

        var user1Opt = postFacade.findMemberByUsername("user1");
        var user2Opt = postFacade.findMemberByUsername("user2");
        var user3Opt = postFacade.findMemberByUsername("user3");

        if (user1Opt.isEmpty() || user2Opt.isEmpty() || user3Opt.isEmpty()) {
            log.info("PostMembers not found. Skipping post creation. (Members sync required via Kafka)");
            return;
        }

        PostMember user1Member = user1Opt.get();
        PostMember user2Member = user2Opt.get();
        PostMember user3Member = user3Opt.get();

        RsData<Post> post1RsData = postFacade.write(user1Member, "제목1", "내용1");
        log.debug(post1RsData.getMsg());

        RsData<Post> post2RsData = postFacade.write(user1Member, "제목2", "내용2");
        log.debug(post2RsData.getMsg());

        RsData<Post> post3RsData = postFacade.write(user1Member, "제목3", "내용3");
        log.debug(post3RsData.getMsg());

        RsData<Post> post4RsData = postFacade.write(user2Member, "제목4", "내용4");
        log.debug(post4RsData.getMsg());

        RsData<Post> post5RsData = postFacade.write(user2Member, "제목5", "내용5");
        log.debug(post5RsData.getMsg());

        RsData<Post> post6RsData = postFacade.write(user3Member, "제목6", "내용6");
        log.debug(post6RsData.getMsg());
    }

    @Transactional
    public void makeBasePostComments() {
        var post1Opt = postFacade.findById(1);
        if (post1Opt.isEmpty()) {
            log.info("Posts not found. Skipping comment creation.");
            return;
        }

        Post post1 = post1Opt.get();
        if (post1.hasComments()) return;

        var user1Opt = postFacade.findMemberByUsername("user1");
        var user2Opt = postFacade.findMemberByUsername("user2");
        var user3Opt = postFacade.findMemberByUsername("user3");

        if (user1Opt.isEmpty() || user2Opt.isEmpty() || user3Opt.isEmpty()) {
            log.info("PostMembers not found. Skipping comment creation.");
            return;
        }

        PostMember user1Member = user1Opt.get();
        PostMember user2Member = user2Opt.get();
        PostMember user3Member = user3Opt.get();

        Post post2 = postFacade.findById(2).get();
        Post post3 = postFacade.findById(3).get();
        Post post4 = postFacade.findById(4).get();

        post1.addComment(user1Member, "댓글1");
        post1.addComment(user2Member, "댓글2");
        post1.addComment(user3Member, "댓글3");

        post2.addComment(user2Member, "댓글4");
        post2.addComment(user2Member, "댓글5");

        post3.addComment(user3Member, "댓글6");
        post3.addComment(user3Member, "댓글7");

        post4.addComment(user1Member, "댓글8");
    }
}
