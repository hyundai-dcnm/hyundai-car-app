# Pleos SDK는 AIDL/리플렉션을 쓰므로 난독화 시 keep 규칙이 필요할 수 있다. release에서 minify를 켤 때 추가한다.
-keep class ai.pleos.** { *; }
