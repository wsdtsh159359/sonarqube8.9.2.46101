/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.monitoring;

import java.util.Locale;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class EsStateSection implements SystemInfoSection {

  private final EsClient esClient;

  public EsStateSection(EsClient esClient) {
    this.esClient = esClient;
  }

  private ClusterHealthStatus getStateAsEnum() {
    return esClient.clusterHealth(new ClusterHealthRequest()).getStatus();
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Search State");
    try {
      setAttribute(protobuf, "State", getStateAsEnum().name());
      completeNodeAttributes(protobuf);
    } catch (Exception es) {
      Loggers.get(EsStateSection.class).warn("Failed to retrieve ES attributes. There will be only a single \"state\" attribute.", es);
      setAttribute(protobuf, "State", es.getCause() instanceof ElasticsearchException ? es.getCause().getMessage() : es.getMessage());
    }
    return protobuf.build();
  }

  private void completeNodeAttributes(ProtobufSystemInfo.Section.Builder protobuf) {
    NodeStatsResponse nodesStatsResponse = esClient.nodesStats();

    if (!nodesStatsResponse.getNodeStats().isEmpty()) {
      toProtobuf(nodesStatsResponse.getNodeStats().get(0), protobuf);
    }
  }

  public static void toProtobuf(NodeStats stats, ProtobufSystemInfo.Section.Builder protobuf) {
    setAttribute(protobuf, "CPU Usage (%)", stats.getCpuUsage());
    setAttribute(protobuf, "Disk Available", byteCountToDisplaySize(stats.getDiskAvailableBytes()));
    setAttribute(protobuf, "Store Size", byteCountToDisplaySize(stats.getIndicesStats().getStoreSizeInBytes()));
    setAttribute(protobuf, "Translog Size", byteCountToDisplaySize(stats.getIndicesStats().getTranslogSizeInBytes()));
    setAttribute(protobuf, "Open File Descriptors", stats.getOpenFileDescriptors());
    setAttribute(protobuf, "Max File Descriptors", stats.getMaxFileDescriptors());
    setAttribute(protobuf, "JVM Heap Usage", formatPercent(stats.getJvmStats().getHeapUsedPercent()));
    setAttribute(protobuf, "JVM Heap Used", byteCountToDisplaySize(stats.getJvmStats().getHeapUsedInBytes()));
    setAttribute(protobuf, "JVM Heap Max", byteCountToDisplaySize(stats.getJvmStats().getHeapMaxInBytes()));
    setAttribute(protobuf, "JVM Non Heap Used", byteCountToDisplaySize(stats.getJvmStats().getNonHeapUsedInBytes()));
    setAttribute(protobuf, "JVM Threads", stats.getJvmStats().getThreadCount());
    setAttribute(protobuf, "Field Data Memory", byteCountToDisplaySize(stats.getIndicesStats().getFieldDataMemorySizeInBytes()));
    setAttribute(protobuf, "Field Data Circuit Breaker Limit",
      byteCountToDisplaySize(stats.getFieldDataCircuitBreakerLimit()));
    setAttribute(protobuf, "Field Data Circuit Breaker Estimation",
      byteCountToDisplaySize(stats.getFieldDataCircuitBreakerEstimation()));
    setAttribute(protobuf, "Request Circuit Breaker Limit",
      byteCountToDisplaySize(stats.getRequestCircuitBreakerLimit()));
    setAttribute(protobuf, "Request Circuit Breaker Estimation",
      byteCountToDisplaySize(stats.getRequestCircuitBreakerEstimation()));
    setAttribute(protobuf, "Query Cache Memory", byteCountToDisplaySize(stats.getIndicesStats().getQueryCacheMemorySizeInBytes()));
    setAttribute(protobuf, "Request Cache Memory",
      byteCountToDisplaySize(stats.getIndicesStats().getRequestCacheMemorySizeInBytes()));
  }

  private static String formatPercent(long amount) {
    return format(Locale.ENGLISH, "%.1f%%", 100.0 * amount * 1.0 / 100.0);
  }
}
