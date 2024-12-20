package ca.exponet.firebolt.job
import ca.exponet.firebolt.APIQueueService
import com.agileorbit.schwartz.SchwartzJob
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.TriggerKey
import org.quartz.TriggerBuilder
import org.quartz.SimpleScheduleBuilder
import java.text.SimpleDateFormat
@CompileStatic
@Slf4j
class APIQueueJobService implements SchwartzJob {
    private static final String name = "clearAPIQueueJob"
    private static final String group = "Firebolt"
    private static final String triggerName = "ClearAPIQueueTrigger"
    APIQueueService APIQueueService
    @Transactional
    void execute(JobExecutionContext context) throws JobExecutionException {
        log.info "Executing Job Trigger {} at {}",
                context.trigger.key,
                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date())
        APIQueueService.processQueue()
    }
    @Override
    void buildTriggers() {
        triggers << TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(triggerName, group))
                .forJob(name, group) // Associate the trigger with the job
                .startAt(new Date(System.currentTimeMillis() + 2 * 60 * 1000)) // Triggered 2 minutes after server startup
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow()
                )
                .build()
    }
    @Override
    String getJobName() {
        return name
    }
    @Override
    String getJobGroup() {
        return group
    }
}