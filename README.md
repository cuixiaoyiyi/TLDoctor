# TLDoctor
ThreadLocal Study and Analysis

# ThreadLocal Misuse Detector

A specialized tool focused on analyzing and detecting improper usage patterns of Java `ThreadLocal`. In complex thread pool or web container environments, improper use of `ThreadLocal` can easily lead to memory leaks, class loader retention, or context pollution. This tool accurately pinpoints these risks at the code level, ensuring system robustness.

Currently, the tool supports the detection of four classic `ThreadLocal` misuse/leak scenarios:

## Detected Misuse Types

### Type I (SCO - Scope-Cleanup Omission)
**Feature:** Developers cache large objects (e.g., byte buffers, `SimpleDateFormat`) in `ThreadLocal` to avoid frequent creation. In thread‑pooled environments, failure to explicitly clean up these objects leads to heap exhaustion (OOM).

### Type II (CLP – ClassLoader Pinning)
**Feature:** In web containers (e.g., Tomcat), threads created by the System/Common ClassLoader hold objects loaded by `WebappClassLoader` via `ThreadLocal`. This prevents the class loader from being garbage collected during hot deployment, causing Metaspace overflow.

### Type III (CPM – Context-Propagation Mismatch)
**Feature:** User identity, tenant ID, or similar context is bound to a `ThreadLocal`. If `remove()` is forgotten after request completion, reused threads read stale data, leading to privilege escalation, data leakage, and other severe business logic errors.

### Type IV (HVR – Heavyweight-Value Retention)
**Feature:** The `ThreadLocal` value is defined as a non‑static inner class. Because the inner class implicitly holds a strong reference to the outer class instance, the outer instance can never be garbage collected as long as the thread remains alive.

## Environment
- Java 23
- Maven

## Usage
```
  mvn compile

  java -jar TLDetector.jar --jar /path/to/your/target.jar
```

## File Structure
- Main file: `TLJarScannerMain.java`
- Detector for Misuse Type I: `SootTypeIDetector.java`
- Detector for Misuse Type II: `SootTypeIIDetector.java`
- Detector for Misuse Type III: `SootTypeIIIDetector.java`
- Detector for Misuse Type IV: `SootTypeIVDetector.java`

## Reported ISSUEs




| Project | Project Link | Stars | Forks | ISSUE ID | ISSUE Description | Create Time | Fix Time | Commit ID |
|---------|--------------|-------|-------|----------|-------------------|-------------|----------|-----------|
| quickfix-j/quickfixj | https://github.com/quickfix-j/quickfixj | 1.1k | 661 | #1137 | Memory leak due to unbounded StringBuilder reuse in Message.class ThreadLocal | 2026.2.11 | 2026.3.10 | 43ecb40 |
| redis/redis-om-spring | https://github.com/redis/redis-om-spring | 648 | 104 | #718 | [Bug] ThreadLocal Context Pollution: Exposed setContext in RedisIndexContext risks cross-tenant data overwriting | 2026.2.21 | 2026.3.10 | 6a5b266 |
| ben-manes/caffeine | https://github.com/ben-manes/caffeine | 17.6k | 1.7k | #1944 | [Bug]: Severe ClassLoader Pinning and Context Pollution via nullBulkLoad ThreadLocal | 2026.3.2 | 2026.3.3 | f29c1cf |
| apache/karaf | https://github.com/apache/karaf | 708 | 663 | #2278 | ClassLoader leak and "Zombie Bundles" caused by static ThreadLocals in XmlUtils | 2026.2.10 | N/A | N/A |
| micrometer-metrics/micrometer | https://github.com/micrometer-metrics/micrometer | 4.8k | 1.1k | #7184 | ClassLoader leak (Metaspace OOM) caused by static ThreadLocals in DoubleFormat | 2026.2.11 | N/A | N/A |
| neo4j/neo4j-ogm | https://github.com/neo4j/neo4j-ogm | 356 | 166 | #1395 | ClassLoader leak (Metaspace OOM) due to static ThreadLocal in DefaultParameterConversion enum | 2026.2.11 | 2026.2.23 | 8f7b220 |
| Azure/azure-sdk-for-java | https://github.com/Azure/azure-sdk-for-java | 2.6k | 2.2k | #48018 | [BUG]Potential Memory Leak: Static ThreadLocal<Collator> in StorageSharedKeyCredential is never cleaned up | 2026.2.17 | 2026.2.19 | 582869e |
| togglz/togglz | https://github.com/togglz/togglz | 1k | 261 | #1344 | [BUG] ClassLoader Leak / Metaspace OOM: ThreadLocalUserProvider.release() incorrectly uses set(null) instead of remove() | 2026.2.18 | 2026.2.21 | 7ffbec0 |
| vsilaev/tascalate-javaflow | https://github.com/vsilaev/tascalate-javaflow | 89 | 7 | #15 | [BUG] ClassLoader Leak / Metaspace OOM: Unmanaged static ThreadLocal (threadMap) in StackRecorder | 2026.2.19 | 2026.3.10 | N/A |
| spotify-web-api-java/spotify-web-api-java | https://github.com/spotify-web-api-java/spotify-web-api-java | 1.1k | 289 | #450 | [Bug] Memory Leak (Unmanaged ThreadLocal) in SpotifyApi via SIMPLE_DATE_FORMAT | 2026.2.20 | 2026.2.20 | 09d6053 |
| LWJGL/lwjgl3 | https://github.com/LWJGL/lwjgl3 | 5.3k | 688 | #1109 | Potential Direct Memory Leak: Missing ThreadLocal.remove() for MemoryStack causes off-heap memory exhaustion in thread pools | 2026.2.21 | N/A | N/A |
| DataDog/java-dogstatsd-client | https://github.com/DataDog/java-dogstatsd-client | 185 | 107 | #292 | Potential ClassLoader Leak: Anonymous inner classes in ThreadLocals pin WebappClassLoader leading to Metaspace OOM | 2026.2.21 | N/A | N/A |
| seasarorg/dbflute | https://github.com/seasarorg/dbflute | 22 | 6 | #8 | [Bug] Severe Context Pollution and Privilege Escalation Risk via Unmanaged ThreadLocal in AccessContext | 2026.2.20 | 2026.2.22 | d7e936a |
| apache/poi | https://github.com/apache/poi | 2.2k | 821 | #1015 | [Security/Bug] Clear-text Password Leak in Server Environments via Unmanaged ThreadLocal in Biff8EncryptionKey | 2026.2.20 | 2026.3.5 | 811eb4a |
| influxdata/influxdb-java | https://github.com/influxdata/influxdb-java | 1.2k | 473 | #1019 | [Bug] Memory Leak (Old Gen Exhaustion) via ThreadLocal StringBuilder in Point.java | 2026.2.22 | 2026.2.26 | 455ee29 |
| mybatis/redis-cache | https://github.com/mybatis/redis-cache | 407 | 212 | #351 | [BUG] KryoSerializer static ThreadLocal leads to ClassLoader Pinning / Metaspace Leak | 2026.2.21 | 2026.3.8 | 649760b |
| apache/bookkeeper | https://github.com/apache/bookkeeper | 2k | 962 | #4714 | [Cleanup]: Remove unnecessary ThreadLocal StringBuilder in LegacyHierarchicalLedgerManager | 2026.3.2 | N/A | N/A |
| crotwell/seisFile | https://github.com/crotwell/seisFile | 35 | 20 | #41 | ClassLoader leak / Metaspace OOM caused by static ThreadLocal in DataRecord | 2026.2.11 | 2026.2.17 | 6380f1b |
| apache/iceberg | https://github.com/apache/iceberg | 8.7k | 3.1k | #15284 | ThreadLocal capacity leak in CommitMetadata due to improper cleanup | 2026.2.10 | 2026.3.9 | 7c210d3 |
| cadence-workflow/cadence-java-client | https://github.com/cadence-workflow/cadence-java-client | 148 | 121 | #1047 | Potential ThreadLocal leak in POJOLocalActivityImplementation due to exception before try-finally | 2026.2.10 | N/A | N/A |
| reportportal/client-java | https://github.com/reportportal/client-java | 25 | 29 | #314 | Memory Leak and ThreadLocal Pollution in LoggingContext due to improper cleanup mechanisms | 2026.2.10 | N/A | N/A |
| duzechao/OKHttpUtils | https://github.com/duzechao/OKHttpUtils | 286 | 82 | #10 | ClassLoader/Context leak caused by static ThreadLocal in HttpDate | 2026.2.10 | N/A | N/A |
| cglib/cglib | https://github.com/cglib/cglib | 4.9k | 886 | #232 | ClassLoader leak and context pollution due to improper ThreadLocal cleanup in AbstractClassGenerator | 2026.2.10 | N/A | N/A |
| beanshell/beanshell | https://github.com/beanshell/beanshell | 930 | 184 | #785 | ThreadLocal pollution in Interpreter.DEBUG causes persistent debug mode and performance degradation | 2026.2.10 | N/A | N/A |
| Netflix/Hystrix | https://github.com/Netflix/Hystrix | 24.5k | 4.7k | #2117 | ThreadLocal pollution in HystrixTimer causing metric attribution errors due to unsafe thread reuse | 2026.2.10 | N/A | N/A |
| Blankj/AndroidUtilCode | https://github.com/Blankj/AndroidUtilCode | 33.7k | 10.7k | #1848 | NumberUtils formatting fails to update on system language change due to static ThreadLocal caching | 2026.2.11 | N/A | N/A |
| square/reader-sdk-flutter-plugin | https://github.com/square/reader-sdk-flutter-plugin | 89 | 31 | #122 | Memory leak caused by static ThreadLocal in DateFormatUtils | 2026.2.11 | N/A | N/A |
| jitsi/jitsi-utils | https://github.com/jitsi/jitsi-utils | 19 | 38 | #155 | Static ThreadLocals in TimeUtils cause ClassLoader leaks and inhibit hot-swapping | 2026.2.11 | N/A | N/A |
| h2database/h2database | https://github.com/h2database/h2database | 4.6k | 1.3k | #4326 | Security Vulnerability: Connection hijacking possible via uncleaned static ThreadLocal | 2026.2.11 | N/A | N/A |
| apache/shiro | https://github.com/apache/shiro | 4.4k | 2.3k | #2560 | ThreadContext state pollution causes Identity Leak / Privilege Escalation in thread pools | 2026.2.11 | N/A | N/A |
| dcm4che/dcm4che | https://github.com/dcm4che/dcm4che | 1.4k | 691 | #1560 | ClassLoader Leak / Metaspace OOM due to uncleaned static ThreadLocal in SpecificCharacterSet | 2026.2.18 | N/A | N/A |
| karatelabs/karate | https://github.com/karatelabs/karate | 8.8k | 2k | #2745 | [BUG] Severe Memory & Data Leak: Unmanaged static ThreadLocal in ScenarioEngine | 2026.2.18 | N/A | N/A |
| scanban/traceragent | https://github.com/scanban/traceragent | 1 | 0 | #1 | [BUG] ClassLoader Leak / Metaspace OOM: Uncleaned static ThreadLocal in MethodCallProcessor | 2026.2.19 | N/A | N/A |
| eclipse-ee4j/glassfish | https://github.com/eclipse-ee4j/glassfish | 435 | 170 | #25932 | [BUG] ClassLoader Leak (Metaspace OOM) due to Unmanaged ThreadLocal in SerialContext | 2026.2.19 | N/A | N/A |
| apache/ignite | https://github.com/apache/ignite | 5.1k | 1.9k | #12771 | [BUG] ClassLoader Leak / Metaspace OOM: Unmanaged static ThreadLocal<Cipher> in KeystoreEncryptionSpi | 2026.2.19 | N/A | N/A |
| glowroot/glowroot | https://github.com/glowroot/glowroot | 1.3k | 333 | #1162 | [Bug] Severe ClassLoader Leak (Metaspace OOM) caused by unmanaged FastThreadLocal in Servlet Containers | 2026.2.20 | N/A | N/A |
| elastic/ecs-logging-java | https://github.com/elastic/ecs-logging-java | 149 | 78 | #381 | Potential Memory Leak: Unbounded StringBuilder retention via ThreadLocal in EcsJsonSerializer | 2026.2.21 | N/A | N/A |
| apache/parquet-java | https://github.com/apache/parquet-java | 3k | 1.5k | #3398 | [Bug] Potential ClassLoader Leak: ThreadLocal.withInitial lambda in Binary.java pins ClassLoader causing Metaspace OOM | 2026.2.21 | 2026.3.16 | 0d862b1 |
| carnellj/spmia-chapter5 | https://github.com/carnellj/spmia-chapter5 | 33 | 99 | #4 | [Security Bug] ThreadLocal Context Pollution: Missing cleanup in UserContextHolder causes cross-request identity leakage | 2026.2.21 | N/A | N/A |
| hazelcast/hazelcast-code-samples | https://github.com/hazelcast/hazelcast-code-samples | 559 | 604 | #763 | [Security Bug] ThreadLocal Context Pollution: Missing cleanup in UserContextHolder causes cross-request identity leakage | 2026.2.19 | N/A | N/A |
| patrickfav/armadillo | https://github.com/patrickfav/armadillo | 308 | 53 | #58 | [BUG] ClassLoader Leak in AesGcmEncryption due to uncleaned ThreadLocal | 2026.2.21 | N/A | N/A |
| baomidou/mybatis-plus | https://github.com/baomidou/mybatis-plus | 17.3k | 4.4k | #7031 | The DynamicTableNameInnerInterceptor poses a risk of context pollution, potentially leading to incorrect data writes | 2026.2.21 | N/A | N/A |
| google/guice | https://github.com/google/guice | 12.7k | 1.7k | #1929 | [Bug] Memory Leak Risk in SingletonScope via Anonymous Provider (Implicit Outer Reference) | 2026.2.21 | N/A | N/A |
| google/allocation-instrumenter | https://github.com/google/allocation-instrumenter | 489 | 88 | #60 | [Bug] Memory Leak via Unintended ThreadLocal (UTL) Misuse in ConstructorInstrumenter | 2026.2.22 | N/A | N/A |
| kabutz/javaspecialists | https://github.com/kabutz/javaspecialists | 147 | 33 | #60 | [Bug] Potential Memory Leak (OOM Risk) via Unmanaged ThreadLocal in ScreenShot.java | 2026.2.22 | N/A | N/A |
| Nike-Inc/wingtips | https://github.com/Nike-Inc/wingtips | 332 | 65 | #141 | [Bug] Critical Context Pollution (Trace Leakage) via unmanaged ThreadLocal in Tracer.java | 2026.2.22 | N/A | N/A |
| kofemann/vfs4j | https://github.com/kofemann/vfs4j | 13 | 3 | #8 | [Bug] Direct Memory Leak and Fragmentation caused by uncleaned ThreadLocal<ByteBuffer> in LocalVFS | 2026.2.22 | N/A | N/A |
| graphql-java/java-dataloader | https://github.com/graphql-java/java-dataloader | 524 | 97 | #266 | [Bug] Memory Leak and Stats Pollution via unmanaged ThreadLocal in ThreadLocalStatisticsCollector | 2026.2.22 | N/A | N/A |
| alibaba/fastjson2 | https://github.com/alibaba/fastjson2 | 4.3k | 554 | #3995 | There is an anti-pattern of ClassLoader Pinning in the Benchmark code involving the ThreadLocal class | 2026.2.22 | N/A | N/A |
| alibaba/DataX | https://github.com/alibaba/DataX | 17.1k | 5.7k | #2346 | [Bug] Critical OS Thread Leak (OOM) via unmanaged ThreadLocal<ExecutorService> in DBUtil.java | 2026.2.22 | N/A | N/A |
| jobrunr/jobrunr | https://github.com/jobrunr/jobrunr | 2.9k | 311 | #1495 | [BUG] Context Pollution (Metadata Leakage) via unmanaged ThreadLocal in ThreadLocalJobContext | 2026.2.22 | N/A | N/A |
| sumanentc/multitenant | https://github.com/sumanentc/multitenant | 122 | 82 | #13 | [BUG] Potential Tenant Identity Leak and Data Cross-Pollination via InheritableThreadLocal in TenantContext | 2026.3.2 | N/A | N/A |
| shevek/parallelgzip | https://github.com/shevek/parallelgzip | 59 | 7 | #13 | Severe Memory Leak (Heap & Off-Heap) via ThreadLocal in ParallelGZIPOutputStream | 2026.3.2 | N/A | N/A |
| lmdbjava/lmdbjava | https://github.com/lmdbjava/lmdbjava | 870 | 126 | #284 | [Bug]: Severe Direct Memory Leak via Unbounded ThreadLocal Queue in ByteBufferProxy | 2026.3.2 | N/A | N/A |
| LWJGL/lwjgl3 | https://github.com/LWJGL/lwjgl3 | 570 | 215 | #169 | [Bug]: Severe ClassLoader Pinning & Memory Leak via Unmanaged ThreadLocals in GLContext | 2026.3.2 | N/A | N/A |
| h2database/h2database | https://github.com/h2database/h2database | 4.6k | 1.3k | #4333 | [Bug]: Severe ClassLoader Pinning & Metaspace OOM via META_LOCK_DEBUGGING ThreadLocals | 2026.3.2 | N/A | N/A |
| UniTime/unitime | https://github.com/UniTime/unitime | 331 | 193 | #220 | [Bug]: Severe Cross-Request Locale Pollution via Unmanaged ThreadLocals in Localization | 2026.3.2 | N/A | N/A |
