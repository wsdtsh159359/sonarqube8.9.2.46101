package org.sonar.ce.configuration;

public class KingdeeWorkerCountProvider implements WorkerCountProvider{

    @Override
    public int get() {
        //TODO API请求获取当前设置的线程数量
        return 8;
    }
}
