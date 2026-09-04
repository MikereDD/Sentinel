import unittest
from sentinel_agent.identity import agent_id, host_id, identity_token

class IdentityTests(unittest.TestCase):
    def test_identity_is_stable(self):
        m = "0123456789abcdef0123456789abcdef"
        self.assertEqual(identity_token(m), identity_token(m))
        self.assertEqual(host_id(m), host_id(m))
        self.assertEqual(agent_id(m), agent_id(m))

    def test_namespaces_are_distinct(self):
        m = "0123456789abcdef0123456789abcdef"
        self.assertTrue(host_id(m).startswith("linux:"))
        self.assertTrue(agent_id(m).startswith("sentinel-agent-linux:"))
        self.assertNotEqual(host_id(m), agent_id(m))

if __name__ == "__main__":
    unittest.main()
