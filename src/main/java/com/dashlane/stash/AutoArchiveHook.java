package com.dashlane.stash;

import com.atlassian.stash.scm.git.*;
import com.atlassian.stash.scm.ScmService;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.event.RepositoryPushEvent;
import com.atlassian.event.api.EventPublisher;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class AutoArchiveHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

    private final GitAgent gitAgent;
    private final EventPublisher eventPublisher;
    private final String tagPrefix = "archive/";


    public AutoArchiveHook(GitAgent gitAgent, EventPublisher eventPublisher){
        this.gitAgent = gitAgent;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        Repository repo = context.getRepository();
        if (!GitScm.ID.equals(repo.getScmId())) {
            return;
        }
        System.out.println("Running Auto Archive Hook");



        for(RefChange rc: refChanges){
            System.out.println("Type: " + rc.getType());
            System.out.println("From hash: " + rc.getFromHash());
            System.out.println("Ref ID: " + rc.getRefId());
            System.out.println("To hash: " + rc.getToHash());

            if(rc.getRefId() != null && rc.getRefId().indexOf("refs/heads/") == 0){
                String branchName = rc.getRefId().substring("refs/heads/".length());
                System.out.println("Branch name: " + branchName);

                if(rc.getType() == RefChangeType.DELETE){
                    // this is a branch delete
                    String tagFullName = "refs/tags/" + tagPrefix + branchName;
                    gitAgent.createRef(repo, tagFullName, rc.getFromHash());

                    // notify stash that we have updated the repo
                    RefChange tagRefChange = new SimpleRefChange.Builder()
                        .refId(tagFullName)
                        .fromHash("0000000000000000000000000000000000000000")
                        .toHash(rc.getFromHash())
                        .type(RefChangeType.ADD).build();
                    eventPublisher.publish(new RepositoryPushEvent(this, repo, Collections.<RefChange>singletonList(tagRefChange)));
                }
            }
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        // TODO: add tagPrefix as a setting here
    }
}
