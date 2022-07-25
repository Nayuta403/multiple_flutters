package dev.flutter.multipleflutters

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel

/**
 * This interface represents the notifications an EngineBindings may be receiving from the Flutter
 * instance.
 *
 * What methods this interface has depends on the messages that are sent over the EngineBinding's
 * channel in `main.dart`.  Messages that interact with the DataModel are handled automatically
 * by the EngineBindings.
 *
 * @see main.dart for what messages are getting sent from Flutter.
 */
interface EngineBindingsDelegate {
    fun onNext()
}

/**
 * This binds a FlutterEngine instance with the DataModel and a channel for communicating with that
 * engine.
 *
 * Messages involving the DataModel are handled by the EngineBindings, other messages are forwarded
 * to the EngineBindingsDelegate.
 *
 * @see main.dart for what messages are getting sent from Flutter.
 */
class EngineBindings(
    activity: Activity,
    delegate: EngineBindingsDelegate,
    entrypoint: String,
    initialRoute: String?,
    initialArguments: Object?
) :
    DataModelObserver {
    lateinit var channel: MethodChannel
    val engine: FlutterEngine
    val delegate: EngineBindingsDelegate

    init {
        val app = activity.applicationContext as App
        // This has to be lazy to avoid creation before the FlutterEngineGroup.
        val dartEntrypoint =
            DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(), entrypoint
            )
        engine = app.engines.createAndRunEngine(activity, dartEntrypoint, initialRoute);
//        engine.dartExecutor.binaryMessenger.enableBufferingIncomingMessages()
        this.delegate = delegate
        Handler(Looper.getMainLooper()).postDelayed({
            Log.i("Nayuta", " multiple-flutters channel register ")
            channel = MethodChannel(engine.dartExecutor.binaryMessenger, "multiple-flutters")
            attach()
        }, 100)

    }

    /**
     * This setups the messaging connections on the platform channel and the DataModel.
     */
    fun attach() {
        DataModel.instance.addObserver(this)
        channel.invokeMethod("setCount", DataModel.instance.counter)
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "incrementCount" -> {
                    DataModel.instance.counter = DataModel.instance.counter + 1
                    result.success(null)
                }
                "next" -> {
                    this.delegate.onNext()
                    result.success(null)
                }
                "test" -> {
                    android.util.Log.i("Nayuta", "multiple-flutters test invoke ")
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    /**
     * This tears down the messaging connections on the platform channel and the DataModel.
     */
    fun detach() {
        // TODO: Uncomment after https://github.com/flutter/engine/pull/24644 is on stable.
        // engine.destroy();
        DataModel.instance.removeObserver(this)
        channel.setMethodCallHandler(null)
    }

    override fun onCountUpdate(newCount: Int) {
        channel.invokeMethod("setCount", newCount)
    }
}
