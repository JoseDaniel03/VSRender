"""CI safeguards for the opt-in 26.3 candidate plugin JAR.

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

    def test_jar_packaging_follows_compilation_with_integrity_checks(self):
        workflow = self.workflow
        compilation = workflow.index("- name: Compile and test (no plugin JAR)")
        packaging = workflow.index("- name: Assemble VSRender 26.3 Paper plugin JAR")
        upload = workflow.index("- name: Upload VSRender 26.3 plugin JAR")
        self.assertLess(compilation, packaging)
        self.assertLess(packaging, upload)
        self.assertIn(":bukkit:paper-loader:shadowJar", workflow[packaging:upload])
        self.assertIn("steps.private_deps.outputs.available == 'true'", workflow[packaging:upload])
        self.assertIn("jar tf", workflow[packaging:upload])
        self.assertIn("unzip -t", workflow[packaging:upload])
        self.assertIn("sha256sum", workflow[packaging:upload])
        self.assertIn("actions/upload-artifact@v4", workflow[upload:])
        self.assertIn("if-no-files-found: error", workflow[upload:])

    def test_one_active_build_per_branch(self):
        source = self.workflow
        self.assertIn("concurrency:", source)
        self.assertIn("vsrender-26-3-${{ github.workflow }}-${{ github.ref }}", source)
        self.assertIn("cancel-in-progress: true", source)

    def test_optional_private_dependency_probe_precedes_core_compilation(self):
        source = self.workflow
        verify = source.index("- name: Verify available original build libraries")
        detect = source.index("- name: Detect optional private compile dependencies")
        probe = source.index("- name: Probe Core tests with available libraries")
        checkout = source.index("- name: Checkout temporary private build libraries")
        self.assertLess(verify, detect)
        self.assertLess(detect, probe)
        self.assertLess(probe, checkout)
        conditional_probe = source[probe:checkout]
        self.assertIn("steps.private_deps.outputs.available != 'true'", conditional_probe)
        self.assertIn(":core:test", conditional_probe)
        self.assertIn("steps.local_libraries.outputs.available", source[detect:probe])

    def test_compile_path_never_requires_committing_private_dependencies(self):
        source = self.workflow
        self.assertIn("persist-credentials: false", source)
        self.assertIn("git hash-object --", source)
        self.assertIn("steps.private_deps.outputs.available == 'true'", source)
        self.assertIn(":bukkit:native26:compileJava", source)
        self.assertIn(":bukkit:paper-loader:compileJava", source)
        self.assertIn("steps.private_deps.outputs.restore == 'true'", source)
        self.assertIn("libs/leafpile-1.0.0.1.jar f1e531a52fc7c029a2964e8206e1b6158b785c88", source)
        self.assertIn("bukkit/compatibility/legacy/libs/awsm-api.jar d5d4d7c4c100edecc6e53f5de0e81683b01a5b0b", source)


if __name__ == "__main__":
    unittest.main()
