package com.itheima.multidown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	private EditText et_url;
	private EditText et_threadcount;
	private LinearLayout ll;
	private static int runningThread;

	// 记录当前线程数量
	private int currentThread;
	private int threadCount;
	private List<ProgressBar> pbs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		et_url = (EditText) findViewById(R.id.et_url);
		et_threadcount = (EditText) findViewById(R.id.et_threadcount);

		ll = (LinearLayout) findViewById(R.id.ll);
	}

	public void click(View v) {

		// TextView tv = new TextView(this);
		// tv.setText("哈哈");
		// ll.addView(tv);
		final String path = et_url.getText().toString().trim();
		String threadString = et_threadcount.getText().toString().trim();
		threadCount = Integer.valueOf(threadString);

		// 在添加view之前，将之前的view清空
		ll.removeAllViews();
		pbs = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			// The specified child already has a parent. You must call
			// removeView() on the child's parent first.
			// 一个子view只能被父布局添加一次，有归属之后不能再次添加
			// View.inflate(this, R.layout.pb, ll);
			ProgressBar pb = (ProgressBar) View
					.inflate(this, R.layout.pb, null);
			ll.addView(pb);
			//将所有的pb添加到集合中
			pbs.add(pb);
			
//			pb.setMax(max);
//			pb.setProgress(progress);
		}
		//下载保存的路径
		final File file = new File(Environment.getExternalStorageDirectory(),
				"fei.exe");
		new Thread(new Runnable() {

			@SuppressWarnings("resource")
			@Override
			public void run() {
				//TODO:下面的这句话主要是起什么作用??
				currentThread = threadCount;
				try {
					URL url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setConnectTimeout(3000);

					int code = conn.getResponseCode();
					if (code == 200) {
						int fileLength = conn.getContentLength();

						RandomAccessFile raf = new RandomAccessFile(file, "rw");
						raf.setLength(fileLength);
						raf.close();

						int block = fileLength / threadCount;
						for (int i = 0; i < threadCount; i++) {
							int startIndex = i * block;
							int endIndex = (i + 1) * block - 1;
							if (i == threadCount - 1) {
								endIndex = fileLength - 1;
							}

							//开启线程
							new DownThread(startIndex, endIndex, i, path).start();
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

	}

	class DownThread extends Thread {
		int startIndex;
		int endIndex;
		int threadId;
		String path;
		
		int maxSize;
		int progressSize = 0;

		public DownThread(int startIndex, int endIndex, int threadId,
				String path) {
			super();
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadId = threadId;
			this.path = path;
			maxSize = endIndex - startIndex;
		}

		@SuppressWarnings("resource")
		@Override
		public void run() {
			File tempFile = new File(Environment.getExternalStorageDirectory()
					+ "/" + threadId + ".txt");
			if (tempFile.exists() && tempFile.length() > 0) {
				try {
					FileInputStream fis = new FileInputStream(tempFile);

					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis));
					String currentString = br.readLine();

					int curentIndex = Integer.valueOf(currentString);
					//进度条初始化
					progressSize = curentIndex - startIndex;
					pbs.get(threadId).setMax(maxSize);
					pbs.get(threadId).setProgress(progressSize);
					
					startIndex = curentIndex;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setConnectTimeout(3000);
				// 设置请求头
				conn.setRequestProperty("Range", "bytes:" + startIndex + "-"
						+ endIndex);

				int code = conn.getResponseCode();
				if (code == 206) {
					InputStream is = conn.getInputStream();

					final File file = new File(
							Environment.getExternalStorageDirectory(), "fei.exe");
					RandomAccessFile raf = new RandomAccessFile(file, "rw");
					// 指针偏移到指定位置开始写入
					raf.seek(startIndex);

					byte[] buffer = new byte[1024 * 8];
					int len = -1;

					int process = 0;
					while ((len = is.read(buffer)) != -1) {
						raf.write(buffer, 0, len);

						process += len;
						
						//设置当前进度
						pbs.get(threadId).setMax(maxSize);
						progressSize += len;
						pbs.get(threadId).setProgress(progressSize);
						
						//记录当前进度的临时文件，为了实现断点下载
						RandomAccessFile rafTemp = new RandomAccessFile(
								tempFile, "rwd");
						int currentPro = startIndex + process;
						rafTemp.write(String.valueOf(currentPro).getBytes());
						rafTemp.close();
					}
					raf.close();
					
					//当所有线程都下载完毕，代表文件下载完了
					synchronized (DownThread.class) {
						currentThread--;
						if (currentThread == 0) {
							for (int i = 0; i < threadCount; i++) {
								File deleteFile = new File(
										Environment
												.getExternalStorageDirectory()
												+ "/" + i + ".txt");
								if (deleteFile.exists()
										&& deleteFile.length() > 0) {
									deleteFile.delete();
								}
							}
							runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(getApplicationContext(), "下载完了！", Toast.LENGTH_SHORT)
									.show();
								}
							});
						}
					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
