# Reglas propias de R8 para el release.
# Room, WorkManager, Firebase y ML Kit traen sus propias reglas dentro de cada librería.
# Los JSON se arman a mano con org.json, entonces no hay modelos que se lean por reflexión.

# Deja el archivo y la línea en los errores para que el stack trace se pueda leer.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ML Kit arma el reconocedor de texto por reflexión al abrir el escáner.
# Sin estas reglas R8 lo deja a medias y la app se cierra al abrir la cámara.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.odml.** { *; }
