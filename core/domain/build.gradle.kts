plugins {
  id("domain")
}

// :core:domain is PURE Kotlin (zero Android dependencies, per design/plan2_0_0.md §2 Agent H).
// It must NOT depend on :core:common — that module is an Android library whose debug/release
// variants are unresolvable from a JVM-only consumer, and an Android dependency here would
// violate the zero-Android-dependency rule for the domain layer.
