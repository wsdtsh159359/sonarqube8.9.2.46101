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
import * as React from 'react';
import { getSources } from '../../../api/components';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { Hotspot } from '../../../types/security-hotspots';
import { constructSourceViewerFile } from '../utils';
import HotspotSnippetContainerRenderer from './HotspotSnippetContainerRenderer';

interface Props {
  branchLike?: BranchLike;
  component: T.Component;
  hotspot: Hotspot;
}

interface State {
  highlightedSymbols: string[];
  lastLine?: number;
  loading: boolean;
  sourceLines: T.SourceLine[];
}

const BUFFER_LINES = 5;
const EXPAND_BY_LINES = 50;

export default class HotspotSnippetContainer extends React.Component<Props, State> {
  mounted = false;
  state: State = {
    highlightedSymbols: [],
    loading: true,
    sourceLines: []
  };

  componentWillMount() {
    this.mounted = true;
    this.fetchSources();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.hotspot.key !== this.props.hotspot.key) {
      this.fetchSources();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkLastLine(lines: T.SourceLine[], target: number): number | undefined {
    if (lines.length < 1) {
      return undefined;
    }
    const lastLineReceived = lines[lines.length - 1].line;
    if (lastLineReceived < target) {
      return lastLineReceived;
    }
    return undefined;
  }

  async fetchSources() {
    const {
      branchLike,
      hotspot: { component, textRange }
    } = this.props;

    if (!textRange) {
      // Hotspot not associated to any loc
      this.setState({ loading: false, lastLine: undefined, sourceLines: [] });
      return;
    }

    const from = Math.max(1, textRange.startLine - BUFFER_LINES);
    // Add 1 to check for end-of-file:
    const to = textRange.endLine + BUFFER_LINES + 1;

    this.setState({ loading: true });

    let sourceLines = await getSources({
      key: component.key,
      from,
      to,
      ...getBranchLikeQuery(branchLike)
    }).catch(() => [] as T.SourceLine[]);

    if (this.mounted) {
      const lastLine = this.checkLastLine(sourceLines, to);

      // remove extra sourceline if we didn't reach the end:
      sourceLines = lastLine ? sourceLines : sourceLines.slice(0, -1);
      this.setState({ lastLine, loading: false, sourceLines });
    }
  }

  handleExpansion = (direction: T.ExpandDirection) => {
    const { branchLike, hotspot } = this.props;
    const { sourceLines } = this.state;

    const range =
      direction === 'up'
        ? {
            from: Math.max(1, sourceLines[0].line - EXPAND_BY_LINES),
            to: sourceLines[0].line - 1
          }
        : {
            from: sourceLines[sourceLines.length - 1].line + 1,
            // Add 1 to check for end-of-file:
            to: sourceLines[sourceLines.length - 1].line + EXPAND_BY_LINES + 1
          };

    return getSources({
      key: hotspot.component.key,
      ...range,
      ...getBranchLikeQuery(branchLike)
    }).then(additionalLines => {
      const { lastLine: previousLastLine } = this.state;

      const lastLine =
        direction === 'down' ? this.checkLastLine(additionalLines, range.to) : previousLastLine;

      let concatSourceLines;
      if (direction === 'up') {
        concatSourceLines = additionalLines.concat(sourceLines);
      } else {
        // remove extra sourceline if we didn't reach the end:
        concatSourceLines = sourceLines.concat(
          lastLine ? additionalLines : additionalLines.slice(0, -1)
        );
      }

      this.setState({
        lastLine,
        sourceLines: concatSourceLines
      });
    });
  };

  handleSymbolClick = (highlightedSymbols: string[]) => {
    this.setState({ highlightedSymbols });
  };

  render() {
    const { branchLike, component, hotspot } = this.props;
    const { highlightedSymbols, lastLine, loading, sourceLines } = this.state;

    const locations = locationsByLine([hotspot]);

    const sourceViewerFile = constructSourceViewerFile(hotspot, lastLine);

    return (
      <HotspotSnippetContainerRenderer
        branchLike={branchLike}
        displayProjectName={component.qualifier === ComponentQualifier.Application}
        highlightedSymbols={highlightedSymbols}
        hotspot={hotspot}
        loading={loading}
        locations={locations}
        onExpandBlock={this.handleExpansion}
        onSymbolClick={this.handleSymbolClick}
        sourceLines={sourceLines}
        sourceViewerFile={sourceViewerFile}
      />
    );
  }
}
