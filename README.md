# MetaioSdk
metaio.sdk maven repo fork


Usage
====================
 * Normal usage
 
Add it to your root build.gradle with:
```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```
and:

```gradle
dependencies {
    compile 'com.github.kolipass:MetaioSdk:6.0.2.1@aar'
}
```

* Sub project dependency

In your project root folder:

```
$ git submodule add https://github.com/kolipass/MetaioSdk MetaioSdk
$ git submodule init
$ git submodule update
$ echo "include ':MetaioSdk:metaioSDK'" >  settings.gradle
```

* Local usage

If you want to use you own local fork:
You need run ```    gradle install ```and check the local Maven repo folder ```.m2/repository/MetaioSdk/MetaioSDK/```

Add it to your root build.gradle with:
```gradle
repositories {
        mavenLocal()
}
```
and:

```gradle
dependencies {
     compile 'MetaioSdk:MetaioSdk:unspecified'
}
```

[![Release](https://img.shields.io/github/release/kolipass/MetaioSdk.svg?label=maven)](https://jitpack.io/#kolipass/MetaioSdk)