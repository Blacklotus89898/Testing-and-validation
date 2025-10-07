#!/usr/bin/env python3
"""
Random Test Runner - Runs tests in random order to verify test independence
"""
import unittest
import random
import sys
import time

def run_tests_randomly():
    """Run tests in random order with a random seed"""
    
    # Generate a random seed for this test run
    seed = random.randint(1, 1000000)
    print(f"🎲 Random seed for this run: {seed}")
    print(f"   To reproduce this exact order, set: random.seed({seed})")
    
    # Set the random seed
    random.seed(seed)
    
    # Load the test suite
    loader = unittest.TestLoader()
    
    try:
        # Load tests from clean_api_tests module
        from clean_api_tests import TestTodosAPI
        suite = loader.loadTestsFromTestCase(TestTodosAPI)
        
        # Get all test methods
        tests = list(suite)
        print(f"📋 Found {len(tests)} test methods")
        
        # Randomize the order
        random.shuffle(tests)
        
        # Show the randomized order
        print("\n🔀 Randomized test execution order:")
        for i, test in enumerate(tests, 1):
            test_name = test._testMethodName
            print(f"   {i}. {test_name}")
        
        print(f"\n{'='*70}")
        
        # Create a new test suite with randomized order
        randomized_suite = unittest.TestSuite(tests)
        
        # Run the tests
        runner = unittest.TextTestRunner(verbosity=2, buffer=True)
        result = runner.run(randomized_suite)
        
        # Print summary
        print(f"\n{'='*70}")
        print(f"🎯 Test Summary (Seed: {seed})")
        print(f"   Tests run: {result.testsRun}")
        print(f"   Failures: {len(result.failures)}")
        print(f"   Errors: {len(result.errors)}")
        
        if result.wasSuccessful():
            print(f"   ✅ All tests passed in random order!")
            print(f"   🎉 Test independence verified!")
        else:
            print(f"   ❌ Some tests failed")
            if result.failures:
                print(f"   💥 Failed tests may have order dependencies")
        
        return result.wasSuccessful()
        
    except ImportError as e:
        print(f"❌ Could not import test module: {e}")
        print(f"   Make sure 'requests' and 'colorama' are installed:")
        print(f"   pip install requests colorama")
        return False
    except Exception as e:
        print(f"❌ Error running tests: {e}")
        return False

if __name__ == "__main__":
    print("🧪 Random Test Order Runner")
    print("="*70)
    
    success = run_tests_randomly()
    
    if not success:
        sys.exit(1)
    
    print(f"\n🔄 Run this script multiple times to test different random orders!")
    print(f"   Each run will use a different random seed.")