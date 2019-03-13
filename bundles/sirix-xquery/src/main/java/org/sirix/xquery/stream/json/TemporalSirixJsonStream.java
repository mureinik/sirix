package org.sirix.xquery.stream.json;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashMap;
import java.util.Map;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.json.TemporalJsonItem;
import org.sirix.api.Axis;
import org.sirix.api.ResourceManager;
import org.sirix.api.json.JsonNodeReadOnlyTrx;
import org.sirix.api.json.JsonNodeTrx;
import org.sirix.axis.AbstractTemporalAxis;
import org.sirix.utils.Pair;
import org.sirix.xquery.json.JsonDBCollection;
import org.sirix.xquery.json.JsonDBNode;
import org.sirix.xquery.node.XmlDBCollection;
import com.google.common.base.MoreObjects;

/**
 * {@link Stream}, wrapping a temporal axis.
 *
 * @author Johannes Lichtenberger
 *
 */
public class TemporalSirixJsonStream implements Stream<TemporalJsonItem> {

  /** Temporal axis. */
  private final AbstractTemporalAxis<JsonNodeReadOnlyTrx, JsonNodeTrx> mAxis;

  /** The {@link JsonDBCollection} reference. */
  private final JsonDBCollection mCollection;

  private final Map<Integer, JsonNodeReadOnlyTrx> mCache;

  /**
   * Constructor.
   *
   * @param axis Sirix {@link Axis}
   * @param collection {@link XmlDBCollection} the nodes belong to
   */
  public TemporalSirixJsonStream(final AbstractTemporalAxis<JsonNodeReadOnlyTrx, JsonNodeTrx> axis,
      final JsonDBCollection collection) {
    mAxis = checkNotNull(axis);
    mCollection = checkNotNull(collection);
    mCache = new HashMap<>();
  }

  @Override
  public TemporalJsonItem next() throws DocumentException {
    if (mAxis.hasNext()) {
      final ResourceManager<JsonNodeReadOnlyTrx, JsonNodeTrx> resourceManager = mAxis.getResourceManager();
      final Pair<Integer, Long> pair = mAxis.next();

      final int revision = pair.getFirst();
      final long nodeKey = pair.getSecond();

      final JsonNodeReadOnlyTrx rtx =
          mCache.computeIfAbsent(revision, revisionNumber -> resourceManager.beginNodeReadOnlyTrx(revisionNumber));
      rtx.moveTo(nodeKey);

      return new JsonDBNode(rtx, mCollection);
    }

    mCache.forEach((revision, rtx) -> rtx.close());
    return null;
  }

  @Override
  public void close() {}

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("axis", mAxis).toString();
  }
}
