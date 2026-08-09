tasks.register("printVersion") {
    doLast {
        val versionFile = file("version.properties")
        val versionProps = java.util.Properties()
        versionProps.load(java.io.FileInputStream(versionFile))
        val major = versionProps["MAJOR"].toString().toInt()
        val minor = versionProps["MINOR"].toString().toInt()
        val patch = versionProps["PATCH"].toString().toInt()
        val buildNum = versionProps["BUILD"].toString().toInt()
        val appVersionCode = major * 1000000 + minor * 10000 + patch * 100 + buildNum
        val appVersionName = "${major}.${minor}.${patch}.${buildNum}"
        println("versionName: $appVersionName")
        println("versionCode: $appVersionCode")
    }
}
