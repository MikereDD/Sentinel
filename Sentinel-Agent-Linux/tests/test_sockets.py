import unittest
from sentinel_agent.sockets import parse_endpoint

class EndpointTests(unittest.TestCase):
    def test_ipv4(self):
        ep = parse_endpoint("192.168.4.85:22")
        self.assertEqual((ep.address, ep.port), ("192.168.4.85", 22))

    def test_ipv6(self):
        ep = parse_endpoint("[::1]:443")
        self.assertEqual((ep.address, ep.port), ("::1", 443))

    def test_wildcard(self):
        ep = parse_endpoint("*:53")
        self.assertEqual((ep.address, ep.port), ("*", 53))

if __name__ == "__main__":
    unittest.main()
