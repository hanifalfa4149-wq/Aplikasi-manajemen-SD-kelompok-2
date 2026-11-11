package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Helper SwingWorker untuk menjalankan akses Service/Database di thread latar belakang
 * agar Event Dispatch Thread (EDT) tetap responsif.
 */
public final class AsyncTaskRunner {

    private AsyncTaskRunner() {
    }

    public static <T> void run(
            Callable<T> task,
            Runnable onStart,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {
        if (onStart != null) {
            onStart.run();
        }

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (Throwable ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    if (onError != null) {
                        onError.accept(ex);
                    }
                }
            }
        };
        worker.execute();
    }
}

