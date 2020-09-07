package vg.civcraft.mc.civmodcore.locations.spatial.quadtrees;

import vg.civcraft.mc.civmodcore.locations.spatial.IIntBBox2D;

import java.util.Optional;

/**
 * @author psygate
 */
final class AreaQuadTreeNode<T extends IIntBBox2D> extends BaseQuadTreeNode<AreaQuadTreeNode<T>, T> {
	AreaQuadTreeNode(IIntBBox2D bbox, int splitSize) {
		super(bbox, splitSize);
	}

	AreaQuadTreeNode(IIntBBox2D bbox, int splitSize, AreaQuadTreeNode parent) {
		super(bbox, splitSize, parent);
	}

	AreaQuadTreeNode(int minX, int minY, int maxX, int maxY, int splitSize, AreaQuadTreeNode parent) {
		super(minX, minY, maxX, maxY, splitSize, parent);
	}

	@Override
	protected boolean nodeContainsValue(T value) {
		return contains(value);
	}

	@Override
	protected AreaQuadTreeNode selectChild(T value) {
		assert childrenSize() > 0;
		for (int i = 0; i < childrenSize(); i++) {
			AreaQuadTreeNode<T> child = getChild(i);
			if (IIntBBox2D.contains(child, value)) {
				return child;
			}
		}

		return null;
	}

	@Override
	protected Optional<AreaQuadTreeNode<T>> selectChildOpt(T value) {
		AreaQuadTreeNode node = selectChild(value);

		if (node != null) {
			return Optional.of(node);
		} else {
			return Optional.empty();
		}
	}

	@Override
	protected AreaQuadTreeNode createNewNode(int minX, int minY, int maxX, int maxY, int splitSize, AreaQuadTreeNode areaQuadTreeNode) {
		return new AreaQuadTreeNode(minX, minY, maxX, maxY, splitSize, this);
	}
}

