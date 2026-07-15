# Changelog

## [1.2.0](https://github.com/ahincho/nova-java-notifications-quarkus-extension/compare/v1.1.0...v1.2.0) (2026-07-15)


### Features

* **ci:** add workflow_dispatch trigger to enable manual CI runs ([4c98773](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/4c987730bdd617e90ffcf1a545b26385c8dbf31a))
* scaffold nova-notifications-quarkus-extension ([5cdf999](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/5cdf9998d14d6e4c99c8297f070794874e3bb712))


### Bug Fixes

* **build:** add GitHub Packages repo for nova-java-notifications dependency ([afc0ce5](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/afc0ce500410c59dad48bb1a75169e8c78693c2d))
* **ci:** drop redundant push trigger and if guards to clear startup_failure ([073047f](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/073047f36b04932bc7cdc5ab0609dcaabd391d8e))
* **ci:** enable --info --stacktrace on publish to surface silent GH Packages 4xx ([909667c](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/909667c0fc517020ca27f07efcff8ab1800c74a7))
* **ci:** resolve packages read token in shell step (same pattern as spring-boot-starter bb67ea7) ([274981b](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/274981b22b29333f4e15adfe30b72fee2198dd29))
* **ci:** restrict matrix to Java 25 (extension classes are Java 25 bytecode) ([304d24f](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/304d24f21e53a88fc963003535728e7da482a292))
* **ext:** check enabled in applyConfigToBuilder + remove unused import ([60eee11](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/60eee11106208509adb99a384e6b75b369eebd95))
* **ext:** switch @ConfigMapping naming strategy to KEBAB_CASE ([a19d06b](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/a19d06b3c18e8deeb1d3d5339e7a69ecb8f98d46))
* **ext:** switch to TOP-LEVEL @ConfigMapping beans per channel ([28a99dc](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/28a99dc5ebe104e76e7a63e90a679efc8f0b458e))
* honor nova.notifications.enabled=false in Quarkus starter ([3398695](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/3398695e32ae949aeacd8ca6dea6e93f4968b497))
* **publish:** add withSourcesJar + withJavadocJar to publish sources and javadoc to GH Packages ([e5f8f32](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/e5f8f320564e59f808f6a973fd5cb7d1c51d57cd))
* **publish:** pass credentials via -Pgpr.user/-Pgpr.key to bypass ghost publish ([3c86d51](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/3c86d515eb27b871f23fe9774d922e3ec81c6b88))
* **publish:** simplify mavenJava publication to match spring-boot-starter pattern ([8121778](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/8121778f651eeb8a98c6171986f7b0f48ddff924))
* **quarkus:** ship META-INF/jandex.idx so CDI scans extension JAR ([572f8d6](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/572f8d629648f30881ea521ee5632b26731e93a4))
* **release:** align gradle.properties version to 1.1.0 to match release-please manifest ([8848758](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/8848758924a2966b1b0f1994a1563290f6a74280))


### Documentation

* add AI assistance attribution disclosure (R-AI compliance) ([ef052be](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/ef052be37d56811e35df648addf7a2fff697b534))
* **readme:** expand What was built + enhance AI Assistance Attribution ([97646b3](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/97646b31f8820390a7f08d3259bdc0d1df87d2cb))
* replace standalone AI-ATTRIBUTION.md with proper README ([64b431d](https://github.com/ahincho/nova-java-notifications-quarkus-extension/commit/64b431d307e04fea7607a7c7c0a5657168c18c76))
