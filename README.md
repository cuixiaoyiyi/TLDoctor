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

## File Structure
- Main file: `TLJarScannerMain.java`
- Detector for Misuse Type I: `SootTypeIDetector.java`
- Detector for Misuse Type II: `SootTypeIIDetector.java`
- Detector for Misuse Type III: `SootTypeIIIDetector.java`
- Detector for Misuse Type IV: `SootTypeIVDetector.java`
