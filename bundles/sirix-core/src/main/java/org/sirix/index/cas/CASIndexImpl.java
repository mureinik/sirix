package org.sirix.index.cas;

import java.util.Iterator;

import org.sirix.api.NodeReadTrx;
import org.sirix.api.PageReadTrx;
import org.sirix.api.PageWriteTrx;
import org.sirix.index.IndexDef;
import org.sirix.index.SearchMode;
import org.sirix.index.avltree.AVLNode;
import org.sirix.index.avltree.AVLTreeReader;
import org.sirix.index.avltree.keyvalue.CASValue;
import org.sirix.index.avltree.keyvalue.NodeReferences;
import org.sirix.index.path.PathFilter;
import org.sirix.index.path.summary.PathSummaryReader;
import org.sirix.node.interfaces.Record;
import org.sirix.page.UnorderedKeyValuePage;

import com.google.common.base.Optional;

public final class CASIndexImpl implements CASIndex<CASValue, NodeReferences> {

	@Override
	public CASIndexBuilder createBuilder(NodeReadTrx rtx,
			PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
			PathSummaryReader pathSummaryReader, IndexDef indexDef) {
		return new CASIndexBuilder(rtx, pageWriteTrx, pathSummaryReader, indexDef);
	}

	@Override
	public CASIndexListener createListener(
			PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
			PathSummaryReader pathSummaryReader, IndexDef indexDef) {
		return new CASIndexListener(pageWriteTrx, pathSummaryReader, indexDef);
	}

	@Override
	public Iterator<NodeReferences> openIndex(PageReadTrx pageReadTrx,
			Optional<CASValue> key, IndexDef indexDef, SearchMode mode, PathFilter filter) {
		final AVLTreeReader<CASValue, NodeReferences> reader = AVLTreeReader.getInstance(pageReadTrx, indexDef.getType(), indexDef.getID());

		final Iterator<AVLNode<CASValue, NodeReferences>> iter = reader.new AVLNodeIterator();
		
		return null;
	}
}
