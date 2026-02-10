package com.back.boundedContext.payout.in;

import com.back.boundedContext.payout.app.PayoutFacade;
import com.back.boundedContext.payout.domain.PayoutPolicy;
import com.back.standard.ut.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@Slf4j
@Profile("!prod")
public class PayoutDataInit {
    private static final int WAIT_SECONDS = 60;
    private static final int RETRY_INTERVAL_MS = 1000;

    private final PayoutDataInit self;
    private final PayoutFacade payoutFacade;
    private final JobOperator jobOperator;
    private final Job payoutCollectItemsAndCompletePayoutsJob;

    public PayoutDataInit(
            @Lazy PayoutDataInit self,
            PayoutFacade payoutFacade,
            JobOperator jobOperator,
            Job payoutCollectItemsAndCompletePayoutsJob
    ) {
        this.self = self;
        this.payoutFacade = payoutFacade;
        this.jobOperator = jobOperator;
        this.payoutCollectItemsAndCompletePayoutsJob = payoutCollectItemsAndCompletePayoutsJob;
    }

    @Bean
    public ApplicationRunner payoutDataInitApplicationRunner() {
        return args -> {
            if (!waitForMemberSync()) {
                return;
            }
            if (waitForPayoutCandidateItems()) {
                self.forceMakePayoutReadyCandidatesItems();
                self.collectPayoutItemsMore();
                self.completePayoutsMore();
                self.runCollectItemsAndCompletePayoutsBatchJob();
            }
        };
    }

    private boolean waitForMemberSync() {
        log.info("Waiting up to {}s for member sync...", WAIT_SECONDS);

        int maxRetries = WAIT_SECONDS * 1000 / RETRY_INTERVAL_MS;
        for (int i = 0; i < maxRetries; i++) {
            if (payoutFacade.findMemberByUsername("user1").isPresent()) {
                log.info("Member sync completed.");
                return true;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Member sync timeout after {}s. Skipping data init.", WAIT_SECONDS);
        return false;
    }

    private boolean waitForPayoutCandidateItems() {
        log.info("Waiting up to {}s for payout candidate items...", WAIT_SECONDS);

        int maxRetries = WAIT_SECONDS * 1000 / RETRY_INTERVAL_MS;
        for (int i = 0; i < maxRetries; i++) {
            if (!payoutFacade.findPayoutCandidateItems().isEmpty()) {
                log.info("Payout candidate items ready. Proceeding with data init.");
                return true;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Payout candidate items timeout after {}s. Skipping data init.", WAIT_SECONDS);
        return false;
    }

    @Transactional
    public void forceMakePayoutReadyCandidatesItems() {
        payoutFacade.findPayoutCandidateItems().forEach(item -> {
            Util.reflection.setField(
                    item,
                    "paymentDate",
                    LocalDateTime.now().minusDays(PayoutPolicy.PAYOUT_READY_WAITING_DAYS + 1)
            );
        });
    }

    @Transactional
    public void collectPayoutItemsMore() {
        payoutFacade.collectPayoutItemsMore(2);
    }

    @Transactional
    public void completePayoutsMore() {
        payoutFacade.completePayoutsMore(1);
    }

    public void runCollectItemsAndCompletePayoutsBatchJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString(
                        "runDate",
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                .toJobParameters();

        try {
            JobExecution execution = jobOperator.start(payoutCollectItemsAndCompletePayoutsJob, jobParameters);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Job instance already complete", e);
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("Job execution already running", e);
        } catch (InvalidJobParametersException e) {
            log.error("Invalid job parameters", e);
        } catch (JobRestartException e) {
            log.error("job restart exception", e);
        }
    }
}
