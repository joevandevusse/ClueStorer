package org.storer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScraperHelperTest {

  private ScraperHelper helper;

  @BeforeEach
  void setUp() {
    helper = new ScraperHelper();
  }

  // --- Jeopardy! round (base values) ---

  @Test
  void testJeopardyRow1() {
    assertEquals("$200", helper.getDailyDoubleValue("clue_J_1_1"));
  }

  @Test
  void testJeopardyRow2() {
    assertEquals("$400", helper.getDailyDoubleValue("clue_J_1_2"));
  }

  @Test
  void testJeopardyRow3() {
    assertEquals("$600", helper.getDailyDoubleValue("clue_J_1_3"));
  }

  @Test
  void testJeopardyRow4() {
    assertEquals("$800", helper.getDailyDoubleValue("clue_J_1_4"));
  }

  @Test
  void testJeopardyRow5() {
    assertEquals("$1000", helper.getDailyDoubleValue("clue_J_1_5"));
  }

  // --- Double Jeopardy! round (values doubled) ---

  @Test
  void testDoubleJeopardyRow1() {
    assertEquals("$400", helper.getDailyDoubleValue("clue_DJ_1_1"));
  }

  @Test
  void testDoubleJeopardyRow2() {
    assertEquals("$800", helper.getDailyDoubleValue("clue_DJ_1_2"));
  }

  @Test
  void testDoubleJeopardyRow3() {
    assertEquals("$1200", helper.getDailyDoubleValue("clue_DJ_1_3"));
  }

  @Test
  void testDoubleJeopardyRow4() {
    assertEquals("$1600", helper.getDailyDoubleValue("clue_DJ_1_4"));
  }

  @Test
  void testDoubleJeopardyRow5() {
    assertEquals("$2000", helper.getDailyDoubleValue("clue_DJ_1_5"));
  }
}
