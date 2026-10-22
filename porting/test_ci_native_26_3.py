"""CI safeguards: full packaging remains deferred during the 26.3 port.

These tests inspect the declared workflow without requiring a YAML parser or
private build dependencies. They do not prove Java compilation or server runtime.
"""
import unittest
from pathlib import Path

WORKFLOW = (Path(__file__).resolve().parents[1] / ".github/workflows/build.yml")


class Native26WorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_no_plugin_packaging_or_jar_artifact_upload(self):
        self.assertNotIn(":bukkit:paper-loader:shadowJar", self.workflow)
        self.assertNotIn("actions/upload-artifact", self.workflow)

    def test_optional_private_dependency_probe_precedes_core_compilation(self):
        source = self.workflow
        detect = source.index("- name: Detect optional private compile dependencies")
        probe = source.index("- name: Probe Core Java compilation without private libraries")
        checkout = source.index("- name: Checkout temporary private build libraries")
        self.assertLess(detect, probe)
        self.assertLess(probe, checkout)
        conditional_probe = source[probe:checkout]
        self.assertIn("steps.private_deps.outputs.available != 'true'", conditional_probe)

    def test_compile_path_never_requires_committing_private_dependencies(self):
        source = self.workflow
        self.assertIn("persist-credentials: false", source)
        self.assertIn("git hash-object --", source)
        self.assertIn("steps.private_deps.outputs.available == 'true'", source)
        self.assertIn(":bukkit:native26:compileJava", source)
        self.assertIn(":bukkit:paper-loader:compileJava", source)


if __name__ == "__main__":
    unittest.main()
