package org.sirix.xquery.function.jn.index.scan;

import com.google.common.collect.ImmutableSet;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.function.json.JSONFun;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.annotation.FunctionAnnotation;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.type.AnyJsonItemType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;
import org.sirix.access.trx.node.json.JsonIndexController;
import org.sirix.api.json.JsonNodeReadOnlyTrx;
import org.sirix.index.IndexDef;
import org.sirix.index.IndexType;
import org.sirix.index.SearchMode;
import org.sirix.index.cas.CASFilter;
import org.sirix.index.path.json.JsonPCRCollector;
import org.sirix.xquery.function.FunUtil;
import org.sirix.xquery.function.sdb.SDBFun;
import org.sirix.xquery.json.JsonDBItem;

import java.util.Set;

/**
 * Scan the CAS-index for matching nodes.
 *
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 *
 */
@FunctionAnnotation(description = "Scans the given CAS index for matching nodes.",
    parameters = {"$doc", "$idx-no", "$key", "$include-self", "$search-mode", "$paths"})
public final class ScanCASIndex extends AbstractScanIndex {

  public final static QNm DEFAULT_NAME = new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "scan-cas-index");

  public ScanCASIndex() {
    super(DEFAULT_NAME,
        new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM, Cardinality.ZeroOrMany), SequenceType.NODE,
            new SequenceType(AtomicType.INR, Cardinality.One), new SequenceType(AtomicType.ANA, Cardinality.One),
            new SequenceType(AtomicType.BOOL, Cardinality.One), new SequenceType(AtomicType.INR, Cardinality.One),
            new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne)),
        true);
  }

  @Override
  public Sequence execute(final StaticContext sctx, final QueryContext ctx, final Sequence[] args) {
    final JsonDBItem doc = (JsonDBItem) args[0];
    final JsonNodeReadOnlyTrx rtx = doc.getTrx();
    final JsonIndexController controller = rtx.getResourceManager().getRtxIndexController(rtx.getRevisionNumber());

    if (controller == null) {
      throw new QueryException(new QNm("Document not found: " + ((Str) args[1]).stringValue()));
    }

    final int idx = FunUtil.getInt(args, 1, "$idx-no", -1, null, true);

    final IndexDef indexDef = controller.getIndexes().getIndexDef(idx, IndexType.CAS);

    if (indexDef == null) {
      throw new QueryException(SDBFun.ERR_INDEX_NOT_FOUND, "Index no %s for collection %s and document %s not found.",
          idx, doc.getCollection().getName(),
          doc.getTrx().getResourceManager().getResourceConfig().getResource().getFileName().toString());
    }
    if (indexDef.getType() != IndexType.CAS) {
      throw new QueryException(SDBFun.ERR_INVALID_INDEX_TYPE,
          "Index no %s for collection %s and document %s is not a CAS index.", idx, doc.getCollection().getName(),
          doc.getTrx().getResourceManager().getResourceConfig().getResource().getFileName().toString());
    }

    final Type keyType = indexDef.getContentType();
    final Atomic key = Cast.cast(sctx, (Atomic) args[2], keyType, true);
    FunUtil.getBoolean(args, 3, "$include-low-key", true, true);
    final int[] searchModes = new int[] {-2, -1, 0, 1, 2};
    final int searchMode = FunUtil.getInt(args, 4, "$search-mode", 0, searchModes, true);

    final SearchMode mode;
    switch (searchMode) {
      case -2:
        mode = SearchMode.LOWER;
        break;
      case -1:
        mode = SearchMode.LOWER_OR_EQUAL;
        break;
      case 0:
        mode = SearchMode.EQUAL;
        break;
      case 1:
        mode = SearchMode.GREATER;
        break;
      case 2:
        mode = SearchMode.GREATER_OR_EQUAL;
        break;
      default:
        // May never happen.
        mode = SearchMode.EQUAL;
    }

    final String paths = FunUtil.getString(args, 5, "$paths", null, null, false);
    final CASFilter filter = (paths != null)
        ? controller.createCASFilter(Set.of(paths.split(";")), key, mode, new JsonPCRCollector(rtx))
        : controller.createCASFilter(ImmutableSet.of(), key, mode, new JsonPCRCollector(rtx));

    return getSequence(doc, controller.openCASIndex(doc.getTrx().getPageTrx(), indexDef, filter));
  }

}
