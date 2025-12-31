package com.eisoo.lineage.presto;

import lombok.Getter;

import java.util.*;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/8 13:27
 * @Version:1.0
 */
public class TreeNode<T> implements Iterable<TreeNode<T>> {
    /**
     * 树节点
     */
    @Getter
    public T data;
    /**
     * 父节点，根没有父节点
     */
    public TreeNode<T> parent;
    /**
     * 子节点，叶子节点没有子节点
     */
    public List<TreeNode<T>> children;
    /**
     * 保存了当前节点及其所有子节点，方便查询
     */
    private List<TreeNode<T>> elementsIndex;

    /**
     * 构造函数
     */
    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<TreeNode<T>>();
        this.elementsIndex = new LinkedList<TreeNode<T>>();
        this.elementsIndex.add(this);
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    /**
     * 判断是否为根：根没有父节点
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 判断是否为叶子节点：子节点没有子节点
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * 添加一个子节点
     */
    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        this.registerChildForSearch(childNode);
        return childNode;
    }

    public TreeNode<T> addChild(TreeNode childNode) {
        childNode.parent = this;
        this.children.add(childNode);
        this.registerChildForSearch(childNode);
        return childNode;
    }

    /**
     * 获取当前节点的层
     */
    public int getLevel() {
        if (this.isRoot()) {
            return 0;
        } else {
            return parent.getLevel() + 1;
        }
    }

    /**
     * 递归为当前节点以及当前节点的所有父节点增加新的节点
     */
    private void registerChildForSearch(TreeNode<T> node) {
        elementsIndex.add(node);
        if (parent != null) {
            parent.registerChildForSearch(node);
        }
    }

    /**
     * 从当前节点及其所有子节点中搜索某节点
     */
    public TreeNode<T> findTreeNode(Comparable<T> cmp) {
        for (TreeNode<T> element : this.elementsIndex) {
            T elData = element.data;
            if (cmp.compareTo(elData) == 0) return element;
        }

        return null;
    }


    public TreeNode<T> findChildNode(Comparable<T> cmp) {
        for (TreeNode<T> element : this.getChildren()) {
            T elData = element.data;
            if (cmp.compareTo(elData) == 0) return element;
        }
        return null;
    }


    /**
     * 获取当前节点的迭代器
     */
    public Iterator<TreeNode<T>> iterator() {
        return new TreeNodeIterator<T>(this);
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "[tree data null]";
    }


    /**
     * 获取所有叶子节点的数据
     */
    public Set<TreeNode<T>> getAllLeafs() {
        Set<TreeNode<T>> leafNodes = new HashSet<TreeNode<T>>();
        if (this.children.isEmpty()) {
            leafNodes.add(this);
        } else {
            for (TreeNode<T> child : this.children) {
                leafNodes.addAll(child.getAllLeafs());
            }
        }
        return leafNodes;
    }

    /**
     * 获取所有叶子节点的数据
     */
    public Set<T> getAllLeafData() {
        Set<T> leafNodes = new HashSet<T>();
        if (this.children.isEmpty()) {
            leafNodes.add(this.data);
        } else {
            for (TreeNode<T> child : this.children) {
                leafNodes.addAll(child.getAllLeafData());
            }
        }
        return leafNodes;
    }
}
