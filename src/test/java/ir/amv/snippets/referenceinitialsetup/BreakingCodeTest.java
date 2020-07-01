package ir.amv.snippets.referenceinitialsetup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BreakingCodeTest {

  @InjectMocks
  private BreakingCode underTest;

  @Test
  void getAmir() {
    int result = underTest.getAmir();
    assertEquals(0, result);
  }
}