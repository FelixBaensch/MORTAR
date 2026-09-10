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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Static helper utility for headless JavaFX controller tests. Provides an offscreen {@link Stage} factory (constructed
 * but never shown, so it must be invoked on the FX thread) and a factory that returns a {@link MockedStatic} over
 * {@link GuiUtil} in which every alert entry point is neutralized, so no code path reaches a real JavaFX {@code Alert}
 * (which throws or blocks when run headless). The static mock's default answer returns the neutralized value for the
 * alert entry points and delegates every other {@link GuiUtil} helper to its real implementation, so those stay
 * functional; callers use the returned {@link MockedStatic} in a try-with-resources block. It further offers
 * {@link #runAndDriveModal(Callable, Consumer)}, the single shared seam that
 * drives a blocking {@code showAndWait} construct to completion headlessly (fire handlers, then always close), and
 * {@link #mockDesktop()}, a {@link MockedStatic} over {@link Desktop} so OS-launch handlers do not throw when run
 * headless.
 * <p>
 * This class is {@code final} with a private no-argument constructor and only static members, following the MORTAR
 * utility-class convention, and intentionally carries NO {@code Test} suffix so JUnit does not treat it as a test class.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class FxTestUtil {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * Default width of the trivial offscreen scene, in pixels.
     */
    private static final double DEFAULT_SCENE_WIDTH = 800.0;
    /**
     * Default height of the trivial offscreen scene, in pixels.
     */
    private static final double DEFAULT_SCENE_HEIGHT = 600.0;
    /**
     * Bounded wait (in seconds) applied to the modal-driving helper, mirroring the harness's own 10 s bound so a stuck
     * {@code showAndWait} construct fails fast with an {@link IllegalStateException} instead of hanging the CI build.
     */
    private static final long FX_TIMEOUT_SECONDS = 10L;
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private constructor that prevents instantiation of this static-only utility class.
     */
    private FxTestUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static methods" defaultstate="collapsed">
    /**
     * Constructs a new offscreen {@link Stage} backed by a trivial {@link Scene} over an empty {@link Pane}. The stage
     * is NOT shown; construction alone is enough to exercise FX-thread wiring. This method MUST be called on the JavaFX
     * Application Thread (e.g. inside {@code AbstractFxTestCase.runAndWait}).
     *
     * @return a newly constructed, unshown offscreen stage
     */
    public static Stage newOffscreenStage() {
        Stage tmpStage = new Stage();
        tmpStage.setScene(new Scene(new Pane(), FxTestUtil.DEFAULT_SCENE_WIDTH, FxTestUtil.DEFAULT_SCENE_HEIGHT));
        return tmpStage;
    }
    //
    /**
     * Creates a {@link MockedStatic} over {@link GuiUtil} whose default answer neutralizes every alert entry point so
     * none reaches a real JavaFX {@code Alert}: {@code guiMessageAlert} and {@code guiMessageAlertWithHyperlink} yield
     * {@link Optional#empty()}; {@code guiConfirmationAlert} and {@code guiYesNoCancelConfirmationAlert} yield
     * {@link ButtonType#OK}; the void {@code guiExceptionAlert} and {@code guiExpandableAlert} yield nothing. Every
     * non-alert {@link GuiUtil} helper is delegated to its real implementation via
     * {@link org.mockito.invocation.InvocationOnMock#callRealMethod()}, so those stay functional.
     * <p>
     * The neutralization is expressed as the mock's default {@link org.mockito.stubbing.Answer} rather than as a set of
     * {@code when(() -> GuiUtil.someAlert(...))} stubs over a {@link Mockito#CALLS_REAL_METHODS} mock. That stubbing form
     * is unsafe here: with {@code CALLS_REAL_METHODS}, evaluating the {@code when(...)} verification lambda invokes the
     * real alert method during setup, and {@code guiExpandableAlert} (a hardcoded {@code ERROR} alert type with a
     * null-tolerant body) then reaches {@code Alert.showAndWait()}, opening a stray modal dialog on the JavaFX
     * Application Thread — which the modal-driving window listener would in turn re-enter and mis-cast. Returning the
     * neutralized value directly from the default answer never calls the real alert methods, so no stray dialog opens.
     * The caller is responsible for closing the returned mock, typically via try-with-resources.
     *
     * @return a static mock of {@link GuiUtil} with all alert entry points neutralized and every other helper real
     */
    public static MockedStatic<GuiUtil> mockGuiAlerts() {
        return Mockito.mockStatic(GuiUtil.class, anInvocation -> switch (anInvocation.getMethod().getName()) {
            case "guiMessageAlert", "guiMessageAlertWithHyperlink" -> Optional.empty();
            case "guiConfirmationAlert", "guiYesNoCancelConfirmationAlert" -> ButtonType.OK;
            case "guiExceptionAlert", "guiExpandableAlert" -> null;
            default -> anInvocation.callRealMethod();
        });
    }
    //
    /**
     * Drives a blocking {@code showAndWait} construct to completion headlessly and returns its result. The supplied
     * {@code aConstruct} is expected to build a controller (or open a view) whose stage is displayed via
     * {@link Stage#showAndWait()}, which blocks the JavaFX Application Thread in a nested event loop until that stage is
     * closed; a plain {@code runAndWait} over such a construct would therefore hang to the harness timeout. This helper
     * instead registers a {@link ListChangeListener} on {@link Window#getWindows()}; when the modal stage becomes
     * visible it schedules — via {@link Platform#runLater(Runnable)} so the work runs INSIDE the nested loop — a driver
     * that first invokes {@code aDriver} on the stage (if non-null) so button and close handlers can be fired, and then
     * ALWAYS closes the stage in a {@code finally} block so no orphan window leaks into a sibling test. The construct is
     * invoked on the FX thread and blocks until that close returns; the window listener is always removed in a
     * {@code finally}. A throwable raised by {@code aDriver} on the JavaFX Application Thread is captured and rethrown to
     * the caller (wrapped in a {@link RuntimeException}) so a failing driver can never produce a false-green result by
     * escaping unnoticed into the FX event loop. The outer wait is bounded at {@link #FX_TIMEOUT_SECONDS} seconds (the
     * same bound the harness applies) so a stuck modal fails fast with an {@link IllegalStateException} rather than
     * hanging the CI build; on timeout a best-effort recovery is scheduled on the FX thread (the still-pumping nested
     * {@code showAndWait} loop) that removes the window listener and closes any still-showing stage the listener
     * detected, which unblocks the parked FX thread so a single stuck modal does not poison every sibling test in the
     * fork.
     *
     * @param <T> the type produced by the construct (e.g. the controller instance; may be null for void opens)
     * @param aConstruct the blocking construct to invoke on the JavaFX Application Thread; must not be null
     * @param aDriver a callback fired on the modal stage before it is closed, or null to simply open then close
     * @return the value produced by the construct (may be null)
     * @throws IllegalStateException if the construct does not complete within the bounded timeout, or the waiting thread
     *                               is interrupted
     * @throws RuntimeException if the construct or the driver throws on the JavaFX Application Thread
     */
    public static <T> T runAndDriveModal(Callable<T> aConstruct, Consumer<Stage> aDriver) {
        AtomicReference<T> tmpResult = new AtomicReference<>();
        AtomicReference<Throwable> tmpError = new AtomicReference<>();
        AtomicReference<Throwable> tmpDriverError = new AtomicReference<>();
        AtomicReference<ListChangeListener<Window>> tmpListenerReference = new AtomicReference<>();
        //stages the listener detected, so a timeout recovery can close a still-showing modal; only touched on the FX
        //thread (the listener adds, the recovery reads), a CopyOnWriteArrayList keeps that access safe regardless
        List<Stage> tmpDetectedStages = new CopyOnWriteArrayList<>();
        CountDownLatch tmpDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            ListChangeListener<Window> tmpListener = aChange -> {
                while (aChange.next()) {
                    for (Window tmpWindow : aChange.getAddedSubList()) {
                        if (tmpWindow instanceof Stage tmpStage && tmpWindow.isShowing()) {
                            tmpDetectedStages.add(tmpStage);
                            Platform.runLater(() -> {
                                try {
                                    if (aDriver != null) {
                                        aDriver.accept(tmpStage);
                                    }
                                } catch (Throwable anError) {
                                    tmpDriverError.set(anError);
                                } finally {
                                    tmpStage.close();
                                }
                            });
                        }
                    }
                }
            };
            tmpListenerReference.set(tmpListener);
            Window.getWindows().addListener(tmpListener);
            try {
                tmpResult.set(aConstruct.call());
            } catch (Throwable anError) {
                tmpError.set(anError);
            } finally {
                Window.getWindows().removeListener(tmpListener);
                tmpDone.countDown();
            }
        });
        try {
            if (!tmpDone.await(FxTestUtil.FX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                //best-effort recovery so a single stuck modal does not poison every sibling test in the fork: on the FX
                //thread (whose nested showAndWait loop still pumps runLater tasks) remove the window listener and close
                //any still-showing stage the listener detected, which unblocks the parked showAndWait, before failing.
                Platform.runLater(() -> {
                    ListChangeListener<Window> tmpRegistered = tmpListenerReference.get();
                    if (tmpRegistered != null) {
                        Window.getWindows().removeListener(tmpRegistered);
                    }
                    for (Stage tmpStage : tmpDetectedStages) {
                        if (tmpStage.isShowing()) {
                            tmpStage.close();
                        }
                    }
                });
                throw new IllegalStateException("Modal construct did not complete within the bounded timeout");
            }
        } catch (InterruptedException anInterruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the modal construct to complete", anInterruptedException);
        }
        if (tmpError.get() != null) {
            throw new RuntimeException("Modal construct failed on the JavaFX Application Thread", tmpError.get());
        }
        if (tmpDriverError.get() != null) {
            throw new RuntimeException("Modal driver failed on the JavaFX Application Thread", tmpDriverError.get());
        }
        return tmpResult.get();
    }
    //
    /**
     * Constructs a {@link MockedStatic} over {@link FileUtil} that turns
     * {@link FileUtil#openFilePathInExplorer(String)} into a no-op and delegates every other static call to the real
     * implementation. That method shells out through {@code Runtime.exec} to {@code explorer} (Windows), {@code open}
     * (macOS) or {@code gio} (Linux), so a test that fires a view button wired to it launches a real file-browser
     * window on any machine where that launcher exists. Stubbing only this one call keeps the handler under test on
     * its real code path without spawning a GUI process.
     *
     * @return the mocked static; the caller owns it and must close it (use try-with-resources)
     */
    public static MockedStatic<FileUtil> mockFileExplorerLaunch() {
        return Mockito.mockStatic(FileUtil.class, anInvocation ->
                anInvocation.getMethod().getName().equals("openFilePathInExplorer")
                        ? null
                        : anInvocation.callRealMethod());
    }
    //
    /**
     * Constructs a {@link MockedStatic} over {@link Desktop} whose {@code isDesktopSupported} returns true and whose
     * {@code getDesktop} returns a Mockito mock, so OS-launch handlers do not throw when run headless.
     *
     * @return the mocked static; the caller owns it and must close it (use try-with-resources)
     */
    public static MockedStatic<Desktop> mockDesktop() {
        Desktop tmpDesktop = Mockito.mock(Desktop.class);
        MockedStatic<Desktop> tmpMock = Mockito.mockStatic(Desktop.class);
        tmpMock.when(Desktop::isDesktopSupported).thenReturn(true);
        tmpMock.when(Desktop::getDesktop).thenReturn(tmpDesktop);
        return tmpMock;
    }
    //
    /**
     * Constructs a {@link MainViewController} over the given (real, caller-owned) primary {@link Stage} and application
     * directory, wiring a fresh {@link MainView} and the {@link Configuration} singleton exactly as the production
     * entry point does. This is the single shared construction seam reused by every {@code MainViewController} headless
     * test (the root controller's constructor ends in a NON-blocking {@code primaryStage.show()}, so it is invoked
     * inside a plain {@code AbstractFxTestCase.runAndWait}, NOT {@link #runAndDriveModal(Callable, Consumer)}). This
     * method MUST be called on the JavaFX Application Thread. The caller retains the passed stage and is responsible for
     * hiding it (via {@code stage.hide()}, which does not fire the window close-request handler) once the test is done,
     * so the controller's {@code closeApplication}/{@code System.exit} path is never reached.
     *
     * @param aPrimaryStage the real, caller-owned primary stage the controller will show; must not be null
     * @param anAppDirPath path to an existing application directory (e.g. the per-test temporary {@code user.home})
     * @return the constructed root controller
     * @throws IOException if the {@link Configuration} singleton or the {@link MainView} cannot be initialized
     */
    public static MainViewController newMainViewController(Stage aPrimaryStage, String anAppDirPath) throws IOException {
        MainView tmpMainView = new MainView(Configuration.getInstance());
        return new MainViewController(aPrimaryStage, tmpMainView, anAppDirPath, Configuration.getInstance());
    }
    //</editor-fold>
}
