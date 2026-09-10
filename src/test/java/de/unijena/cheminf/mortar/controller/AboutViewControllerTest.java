/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.AboutView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Headless unit tests for {@link AboutViewController} (COV-06). This controller is a BLOCKING modal: its constructor
 * ends in {@code aboutViewStage.showAndWait()} (see {@code AboutViewController.showAboutView}), which blocks the JavaFX
 * Application Thread in a nested event loop until the stage closes. A plain
 * {@link AbstractFxTestCase#runAndWait(Runnable)} over the construction would therefore hang to the harness timeout, so
 * every test constructs the controller through
 * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}: the construct builds
 * an offscreen owner {@link Stage} on the FX thread and returns the controller, and the driver — scheduled to run INSIDE
 * the nested loop, after the controller's pre-{@code showAndWait} {@code Platform.runLater} (which registers the button
 * handlers, parses the tool-description XML, and binds the table) has already drained — obtains the {@link AboutView}
 * via the modal stage's scene root ({@code (AboutView) stage.getScene().getRoot()}, no production widening).
 * <p>
 * The GitHub and tutorial buttons invoke {@link java.awt.Desktop}, which throws a {@code HeadlessException} on a
 * headless JVM. Those handlers are therefore fired inside a {@code try (MockedStatic<Desktop> ...)} scope so
 * {@code Desktop.getDesktop()} yields a Mockito mock: the GitHub handler's {@code browse} is a no-op (covering the
 * try-success path of {@code openGitHubRepositoryInDefaultBrowser}), while the tutorial handler's {@code open} is
 * stubbed to throw {@link IOException} so the catch branch of {@code openTutorialInDefaultPdfViewer} builds the
 * fallback {@link javafx.scene.control.Hyperlink} and calls the (also mocked) {@code GuiUtil.guiMessageAlertWithHyperlink}.
 * Both static mocks are opened on the JavaFX Application Thread inside the driver, because a Mockito {@code MockedStatic}
 * is active only on the thread that created it and the handlers fire on the FX thread. This is a test-only OS-boundary
 * neutralization consistent with the established {@code GuiUtil} allowance; the XXE-hardened
 * {@code DocumentBuilderFactory} configuration in {@code getExternalToolInfoFromXml} is exercised, never weakened, and
 * no production code is modified.
 * <p>
 * Assertions are behavioral only (controller constructed, tool list populated on the happy path, a single error row on
 * the parse-failure path) and never pin exact CDK-derived strings.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class AboutViewControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public AboutViewControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Drives the blocking About modal to completion under the real {@link Configuration}, asserts the bundled
     * {@code tools_description.xml} parsed into multiple external-tool rows (the happy path of
     * {@code getExternalToolInfoFromXml}), and fires all four view buttons under {@link MockedStatic} over
     * {@link Desktop} and {@code GuiUtil}: the GitHub button (mock {@code browse} no-op → try-success branch), the
     * tutorial button (mock {@code open} throws {@link IOException} → catch branch builds the fallback hyperlink and
     * calls the mocked alert), the log-file button (its call to {@code FileUtil.openFilePathInExplorer} stubbed to a
     * no-op and then verified, so the handler runs its real code path without shelling out to a file browser), and the
     * close button (its stage-close lambda). Exercises the constructor, {@code showAboutView}, {@code addListeners}, the XML happy path,
     * and both OS-launch handlers without a {@code HeadlessException} or a hang.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void happyPathDrivesModalAndFiresAllHandlersTest() throws Exception {
        AtomicInteger tmpToolCount = new AtomicInteger(-1);
        AboutViewController tmpController = this.driveAbout(Configuration.getInstance(), aView -> {
            tmpToolCount.set(aView.getTableView().getItems().size());
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts();
                    MockedStatic<Desktop> tmpDesktopMock = FxTestUtil.mockDesktop();
                    MockedStatic<FileUtil> tmpFileUtilMock = FxTestUtil.mockFileExplorerLaunch()) {
                Desktop tmpDesktop = Desktop.getDesktop();
                Mockito.doThrow(new IOException("headless tutorial open")).when(tmpDesktop).open(Mockito.any(File.class));
                aView.getGitHubButton().fire();
                aView.getTutorialButton().fire();
                aView.getLogFileButton().fire();
                //the handler reaches FileUtil.openFilePathInExplorer, which is stubbed out above: unstubbed it shells
                //out to explorer/open/gio and launches a real file-browser window wherever that launcher exists
                tmpFileUtilMock.verify(() -> FileUtil.openFilePathInExplorer(Mockito.anyString()));
                aView.getCloseButton().fire();
            } catch (IOException anException) {
                //unreachable: the doThrow(...).when(...).open(...) stubbing call declares IOException but never throws
                throw new IllegalStateException("Unexpected checked exception while stubbing Desktop.open", anException);
            }
        });
        Assertions.assertNotNull(tmpController);
        Assertions.assertTrue(tmpToolCount.get() > 1,
                "the bundled tools_description.xml should parse into multiple external-tool rows on the happy path");
    }
    //
    /**
     * Drives the About modal under a delegating {@link IConfiguration} whose {@code mortar.tools.description.name}
     * resolves to a non-existent resource, so {@code getExternalToolInfoFromXml} hits a {@link NullPointerException}
     * when {@code getResource(...)} returns {@code null} and its {@code catch} adds exactly one placeholder error row.
     * The delegate returns the real value for every other key (icon, logo, folders, URLs), so construction still
     * succeeds. Asserts a single row is present and its name equals the localized XML-parsing error message, pinning the
     * catch/fallback branch without weakening the XXE-hardened factory configuration.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void xmlParseFailureAddsSingleErrorRowTest() throws Exception {
        IConfiguration tmpBadConfiguration = new MissingToolsDescriptionConfiguration(Configuration.getInstance());
        AtomicInteger tmpRowCount = new AtomicInteger(-1);
        AtomicReference<ExternalTool> tmpFirstRow = new AtomicReference<>();
        AboutViewController tmpController = this.driveAbout(tmpBadConfiguration, aView -> {
            tmpRowCount.set(aView.getTableView().getItems().size());
            if (!aView.getTableView().getItems().isEmpty()) {
                tmpFirstRow.set(aView.getTableView().getItems().get(0));
            }
        });
        Assertions.assertNotNull(tmpController);
        Assertions.assertEquals(1, tmpRowCount.get(),
                "a failed XML parse should add exactly one placeholder error-row external tool");
        Assertions.assertNotNull(tmpFirstRow.get());
        Assertions.assertEquals(Message.get("AboutViewController.Error.XMLParsing.Name"), tmpFirstRow.get().getName());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Constructs an {@link AboutViewController} through the blocking-modal driver and runs the supplied view driver
     * against the shown modal stage. The construct — invoked on the JavaFX Application Thread by
     * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)} — builds an
     * offscreen owner {@link Stage} and returns the controller whose constructor calls {@code showAndWait}. The driver,
     * run inside the nested event loop after the controller's pre-{@code showAndWait} {@code Platform.runLater} has
     * registered the handlers, parsed the XML, and bound the table, resolves the {@link AboutView} from the modal
     * stage's scene root and delegates to the caller (which may open any {@link MockedStatic} it needs on the FX thread
     * and fire handlers). The FX event queue is drained afterwards so any handler-scheduled work completes before the
     * method returns.
     *
     * @param aConfiguration the configuration passed to the controller (real or a delegating variant)
     * @param aViewDriver the callback fired on the resolved {@link AboutView} inside the nested loop; must not be null
     * @return the constructed controller (after its {@code showAndWait} has returned)
     * @throws Exception if the trailing {@code runAndWait} surfaces a throwable raised on the JavaFX Application Thread
     */
    private AboutViewController driveAbout(IConfiguration aConfiguration, Consumer<AboutView> aViewDriver) throws Exception {
        AboutViewController tmpController = FxTestUtil.runAndDriveModal(
                () -> new AboutViewController(FxTestUtil.newOffscreenStage(), aConfiguration),
                aStage -> {
                    AboutView tmpView = (AboutView) aStage.getScene().getRoot();
                    aViewDriver.accept(tmpView);
                });
        AbstractFxTestCase.waitForFxEvents();
        //surface any throwable an async Platform.runLater raised on the JavaFX Application Thread —
        //waitForFxEvents drains the queue but does not read FX_UNCAUGHT, so a trailing runAndWait rethrows it
        AbstractFxTestCase.runAndWait(() -> { });
        return tmpController;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Nested types" defaultstate="collapsed">
    /**
     * A delegating {@link IConfiguration} that returns the real property value for every key except
     * {@code mortar.tools.description.name}, for which it returns a non-existent resource file name. This makes
     * {@code AboutViewController.getExternalToolInfoFromXml} resolve the tool-description resource to {@code null} and
     * fall into its {@code catch} branch (a {@link NullPointerException} from {@code getResource(...).toExternalForm()}),
     * while leaving icon/logo/folder/URL resolution intact so the controller still constructs. It does not touch or
     * weaken the XXE-hardened {@code DocumentBuilderFactory} configuration; the failure is driven purely through an
     * unresolvable resource path.
     */
    private static final class MissingToolsDescriptionConfiguration implements IConfiguration {
        /**
         * The real configuration every non-overridden key is delegated to.
         */
        private final IConfiguration delegate;
        //
        /**
         * Creates a delegating configuration wrapping the given real configuration.
         *
         * @param aDelegate the configuration to delegate all non-overridden keys to
         */
        private MissingToolsDescriptionConfiguration(IConfiguration aDelegate) {
            this.delegate = aDelegate;
        }
        //
        /**
         * Returns a non-existent file name for the tool-description key so its resource cannot be resolved, and the real
         * value from the delegate for every other key.
         *
         * @param aKey key defined in the configuration properties file
         * @return the overridden non-existent name for the tool-description key, otherwise the delegate's value
         * @throws MissingResourceException if the delegate cannot resolve a non-overridden key
         */
        @Override
        public String getProperty(String aKey) throws MissingResourceException {
            if ("mortar.tools.description.name".equals(aKey)) {
                return "nonexistent_tools_description_file.xml";
            }
            return this.delegate.getProperty(aKey);
        }
    }
    //</editor-fold>
}
