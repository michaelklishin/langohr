package com.novemberain.langohr.recovery;

import java.io.IOException;

public interface RecoverableEntity {
  Object recover() throws IOException;
}
