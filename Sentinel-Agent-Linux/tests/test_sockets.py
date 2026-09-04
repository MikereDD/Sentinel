import unittest
from sentinel_agent.sockets import parse_endpoint

class EndpointTests(unittest.TestCase):
    def test_ipv4(self):
        ep = parse_endpoint("192.168.4.85:22")
        self.assertEqual((ep.address, ep.port, ep.interface), ("192.168.4.85", 22, None))

    def test_ipv6(self):
        ep = parse_endpoint("[::1]:443")
        self.assertEqual((ep.address, ep.port, ep.interface), ("::1", 443, None))

    def test_wildcard(self):
        ep = parse_endpoint("*:53")
        self.assertEqual((ep.address, ep.port, ep.interface), ("*", 53, None))

    def test_ipv4_scope_is_separated(self):
        ep = parse_endpoint("192.168.4.54%end0:68")
        self.assertEqual(ep.address, "192.168.4.54")
        self.assertEqual(ep.port, 68)
        self.assertEqual(ep.interface, "end0")

    def test_loopback_scope_is_separated(self):
        ep = parse_endpoint("127.0.0.53%lo:53")
        self.assertEqual(ep.address, "127.0.0.53")
        self.assertEqual(ep.port, 53)
        self.assertEqual(ep.interface, "lo")

if __name__ == "__main__":
    unittest.main()
