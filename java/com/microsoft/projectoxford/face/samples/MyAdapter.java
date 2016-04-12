package com.microsoft.projectoxford.face.samples;

import android.content.Context;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.List;


public class MyAdapter extends CommonAdapter<String>
{

	/**
	 * 用户选择的图片，存储为图片的完整路径
	 */
	public static List<String> mSelectedImage = new LinkedList<String>();

	/**
	 * 文件夹路径
	 */
	private String mDirPath;

	public MyAdapter(Context context, List<String> mDatas, int itemLayoutId)
	{
		super(context, mDatas, itemLayoutId);
	}

	@Override
	public void convert(final ViewHolder helper, final String item)
	{
		//设置no_pic
		helper.setImageResource(R.id.id_item_image, R.drawable.pictures_no);
		//设置no_selected
//				helper.setImageResource(R.id.id_item_select, R.drawable.picture_unselected);
		//设置图片
		helper.setImageByUrl(R.id.id_item_image, item);
		
		final ImageView mImageView = helper.getView(R.id.id_item_image);

		mImageView.setColorFilter(null);

	}
}
