package com.thordickinson.multiprocess.api;

import java.io.IOException;
import java.util.List;

public interface DataSource<I> extends Initialize {
    List<I> next(int count) throws IOException;
}

