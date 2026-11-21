# Reglas propias de R8 para el release.
# Room, WorkManager, Firebase y ML Kit traen sus propias reglas dentro de cada librería.
# Los JSON se arman a mano con org.json, entonces no hay modelos que se lean por reflexión.

# Deja el archivo y la línea en los errores para que el stack trace se pueda leer.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
