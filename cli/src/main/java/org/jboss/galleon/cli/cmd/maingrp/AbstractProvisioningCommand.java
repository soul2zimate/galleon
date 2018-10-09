/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.galleon.cli.cmd.maingrp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.option.Option;
import org.aesh.readline.AeshContext;
import org.jboss.galleon.Errors;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.ProvisioningManager;
import org.jboss.galleon.cli.CommandExecutionException;
import org.jboss.galleon.cli.HelpDescriptions;
import org.jboss.galleon.cli.PmOptionActivator;
import org.jboss.galleon.cli.PmSession;
import org.jboss.galleon.cli.PmSessionCommand;
import org.jboss.galleon.cli.cmd.CommandDomain;
import org.jboss.galleon.cli.cmd.CommandWithInstallationDirectory;
import org.jboss.galleon.cli.model.FeatureContainer;
import org.jboss.galleon.cli.model.FeatureContainers;
import org.jboss.galleon.config.ProvisioningConfig;
import org.jboss.galleon.layout.FeaturePackLayout;
import org.jboss.galleon.layout.ProvisioningLayout;
import org.jboss.galleon.runtime.ProvisioningRuntime;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class AbstractProvisioningCommand extends PmSessionCommand implements CommandWithInstallationDirectory {

    public static class DirActivator extends PmOptionActivator {

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            return getPmSession().getContainer() == null;
        }
    }

    public static final String DIR_OPTION_NAME = "dir";
    public static final String VERBOSE_OPTION_NAME = "verbose";

    @Option(name = DIR_OPTION_NAME, required = false,
            description = HelpDescriptions.INSTALLATION_DIRECTORY, activator = DirActivator.class)
    protected File targetDirArg;

    protected ProvisioningManager getManager(PmSession session, boolean verbose) throws ProvisioningException {
        Path install = getInstallationDirectory(session.getAeshContext());
        if (!Files.exists(install)) {
            throw new ProvisioningException(Errors.homeDirNotUsable(install));
        }
        return session.newProvisioningManager(install, verbose);
    }

    @Override
    public Path getInstallationDirectory(AeshContext context) {
        return targetDirArg == null ? PmSession.getWorkDir(context) : targetDirArg.toPath();
    }

    public FeatureContainer getFeatureContainer(PmSession session, ProvisioningLayout<FeaturePackLayout> layout) throws ProvisioningException,
            CommandExecutionException, IOException {
        FeatureContainer container;
        ProvisioningManager manager = getManager(session, false);

        if (manager.getProvisionedState() == null) {
            throw new CommandExecutionException("Specified directory doesn't contain an installation");
        }
        if (layout == null) {
            ProvisioningConfig config = manager.getProvisioningConfig();
            try (ProvisioningRuntime runtime = manager.getRuntime(config)) {
                container = FeatureContainers.fromProvisioningRuntime(session, runtime);
            }
        } else {
            try (ProvisioningRuntime runtime = manager.getRuntime(layout)) {
                container = FeatureContainers.fromProvisioningRuntime(session, runtime);
            }
        }
        return container;
    }

    protected ProvisioningConfig getProvisioningConfig(PmSession session) throws ProvisioningException, CommandExecutionException {
        ProvisioningManager manager = getManager(session, false);

        if (manager.getProvisionedState() == null) {
            throw new CommandExecutionException("Specified directory doesn't contain an installation");
        }
        return manager.getProvisioningConfig();
    }

    @Override
    public CommandDomain getDomain() {
        return CommandDomain.PROVISIONING;
    }
}
